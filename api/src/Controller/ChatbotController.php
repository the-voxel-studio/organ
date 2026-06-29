<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\Project;
use App\Entity\Organ;
use App\Entity\ProjectMember;
use App\Entity\User;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

#[Route('/chatbot', name: 'api_chatbot_')]
class ChatbotController extends AbstractController
{
    public function __construct() {}

    #[Route('/message', name: 'message', methods: ['POST'])]
    public function handleMessage(Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        /** @var User|null $user */
        $user = $this->getUser();

        if (!$user) {
            return $this->json(['message' => 'Not authenticated'], Response::HTTP_UNAUTHORIZED);
        }

        // Autorise l'exécution longue pour le timeout de cURL (90s)
        set_time_limit(110);

        // 1. Lire les paramètres de la requête
        $content = json_decode($request->getContent(), true) ?? [];
        $history = $content['history'] ?? [];
        $context = $content['context'] ?? [];
        $currentRoute = $context['currentRoute'] ?? '/';
        $command = $context['command'] ?? 'ask';

        $userMsg = count($history) > 0 ? ($history[count($history) - 1]['content'] ?? '') : '';
        error_log("[Chatbot] User Message: " . $userMsg . " | Route: " . $currentRoute . " | Command: " . $command);

        // 2. Charger les projets pour l'injection initiale dans le prompt
        $projectMembers = $entityManager->getRepository(ProjectMember::class)->findBy([
            'user' => $user,
            'deletedAt' => null
        ]);

        $availableProjects = [];
        foreach ($projectMembers as $pm) {
            $p = $pm->getProject();
            if ($p && $p->getDeletedAt() === null) {
                $availableProjects[] = [
                    'uuid' => $p->getUuid(),
                    'title' => $p->getTitle(),
                    'status' => $p->getStatus()->value
                ];
            }
        }

        // 3. Charger les organes du projet courant
        $availableOrgans = [];
        $projectUuid = null;

        if (preg_match('/\/project\/([a-fA-F0-9-]{36})/', $currentRoute, $matches)) {
            $projectUuid = $matches[1];
        } elseif (preg_match('/\/organ\/([a-fA-F0-9-]{36})\/([a-fA-F0-9-]{36})/', $currentRoute, $matches)) {
            $projectUuid = $matches[1];
        }

        $availableMembers = [];
        if ($projectUuid) {
            $project = $entityManager->getRepository(Project::class)->findOneBy([
                'uuid' => $projectUuid,
                'deletedAt' => null
            ]);
            if ($project) {
                $organs = $entityManager->getRepository(Organ::class)->findBy([
                    'project' => $project,
                    'deletedAt' => null
                ]);
                foreach ($organs as $organ) {
                    $roles = $entityManager->getRepository(\App\Entity\OrganRole::class)->findBy([
                        'organ' => $organ,
                        'deletedAt' => null
                    ]);
                    $organRoles = [];
                    foreach ($roles as $role) {
                        $organRoles[] = [
                            'uuid' => $role->getUuid(),
                            'name' => $role->getName()
                        ];
                    }
                    $availableOrgans[] = [
                        'uuid' => $organ->getUuid(),
                        'title' => $organ->getTitle(),
                        'roles' => $organRoles
                    ];
                }

                $projectMembers = $entityManager->getRepository(ProjectMember::class)->findBy([
                    'project' => $project,
                    'deletedAt' => null
                ]);
                foreach ($projectMembers as $pm) {
                    $u = $pm->getUser();
                    if ($u) {
                        $availableMembers[] = [
                            'uuid' => $u->getUuid(),
                            'name' => $u->getFirstName() . ' ' . $u->getLastName(),
                            'email' => $u->getEmail()
                        ];
                    }
                }
            }
        }

        $currentUserInfo = [
            'uuid' => $user->getUuid(),
            'name' => $user->getFirstName() . ' ' . $user->getLastName(),
            'email' => $user->getEmail()
        ];

        // 4. Récupérer les clés d'API depuis .env
        $apiKey = $_ENV['NVIDIA_API_KEY'] ?? '';
        $model = $_ENV['NVIDIA_MODEL'] ?? 'mistralai/mixtral-8x22b-instruct-v0.1';

        if (empty($apiKey) || str_contains($apiKey, 'your-real-key-here')) {
            return $this->json([
                'message' => "Le service d'assistant IA n'est pas encore configuré. Veuillez contacter l'administrateur pour ajouter la clé NVIDIA_API_KEY dans le fichier .env.",
                'actions' => []
            ]);
        }

        // 5. Définir le system prompt avec sitemap et instructions
        $systemPrompt = "Tu es un tuteur et assistant virtuel intelligent pour l'application de gestion de tâches et projets 'Organ'.
Ton but unique est d'aider et d'accompagner l'utilisateur dans l'usage de cette application, et RIEN D'AUTRE. Refuse poliment toute question hors sujet.

Voici les fonctionnalités majeures de l'application :
- Gestion de projets (titre, couleur, membres, icône emoji).
- Organs (les sections ou colonnes d'un projet, sous forme de tableaux Kanban avec des colonnes de tâches, icône emoji, rôles locaux).
- Rôles locaux d'Organs (nom du rôle, icône emoji, permissions spécifiques).
- Tâches (titre, dates de début et d'échéance, priorités de 1 à 10, sous-tâches, commentaires, pièces jointes, responsable/manager, assignés, dépendances entre tâches, liens Web).
- Analyse de projet (statistiques d'avancement des tâches).
- Corbeille (permet de restaurer des projets/Organs supprimés).

Sitemap de l'application :
- Page d'accueil publique / Landing : /
- Tableau de bord utilisateur connecté : /dashboard
- Paramètres globaux de ton compte : /settings
- Corbeille globale : /trash
- Créer un projet : /project/new
- Voir un projet spécifique : /project/:projectUuid
- Paramètres d'un projet : /project/:projectUuid/settings
- Analytics d'un projet : /project/:projectUuid/analytics
- Créer un Organ dans un projet : /project/:projectUuid/organ/new
- Voir un Organ (tableau Kanban) : /organ/:projectUuid/:organUuid
- Paramètres d'un Organ : /project/:projectUuid/organ/:organUuid/settings
- Corbeille spécifique d'un Organ : /organ/:projectUuid/:organUuid/trash

Voici les données courantes de l'utilisateur connecté récupérées de la base de données :
- Prénom de l'utilisateur : " . $user->getFirstName() . "
- Informations sur l'utilisateur connecté actuel : " . json_encode($currentUserInfo) . "
- Page actuelle dans l'application : " . $currentRoute . "
- Liste des projets accessibles avec leur UUID : " . json_encode($availableProjects) . "
- Liste des Organs du projet actuel : " . json_encode($availableOrgans) . "
- Liste des membres du projet actuel (responsables/assignés possibles pour les tâches) : " . json_encode($availableMembers) . "

RÈGLES DE GESTION DES MEMBRES & RÉPONSES (LOGIQUE MÉTIER OBLIGATOIRE) :
- Logique d'ajout de membre : Pour qu'un membre puisse faire partie d'un Organ, il doit obligatoirement avoir été ajouté au projet parent au préalable. Ensuite seulement, il est possible de l'ajouter dans l'Organ en lui attribuant un rôle local.
- Instructions pas-à-pas en cas de question : En cas de question de l'utilisateur (ex: comment ajouter une personne extérieure au projet dans un Organ), tu ne dois PAS lui donner de liens URL. Tu dois obligatoirement détailler les étapes textuelles suivantes :
  1) Ajouter la personne dans le projet via la page paramètre du projet.
  2) Ajouter la personne avec un rôle au minimum dans l'Organ via le bouton paramètre sur la page de l'Organ.

RÈGLES DE BRANDING CRITIQUES :
- Tu dois IMPÉRATIVEMENT utiliser le mot « Organ » (au singulier) ou « Organs » (au pluriel) pour désigner les sous-sections d'un projet ou l'application. Ne dis JAMAIS « organe » ou « organes » en français, c'est une faute de branding majeure. Écris toujours « Organ » ou « Organs » avec une majuscule.

Consignes de formatage de ta réponse :
Tu dois ABSOLUMENT répondre sous la forme d'un objet JSON strict valide contenant uniquement ces deux clés :
1. \"message\" : Ta réponse textuelle en français (limitée à 1 ou 2 phrases courtes maximum). Tu DOIS utiliser du markdown standard (ex: **texte en gras**, *italique*, listes à puces) pour structurer tes explications. Si tu génères des actions automatiques, décris à l'utilisateur la série d'actions que tu vas exécuter pour lui. Si l'utilisateur pose une question de type /ask ou demande comment faire quelque chose, tu dois lui expliquer comment IL doit le faire (ne dis pas « je vais... » ou « je m'occupe de... » si c'est l'utilisateur qui doit faire l'action).
2. \"actions\" : Un tableau d'actions JSON à exécuter séquentiellement par le front-end, ou un tableau vide [].
   Chaque action peut être de type :
   - Navigation : { \"type\": \"navigate\", \"path\": \"/chemin\" } (Le chemin peut utiliser des variables temporaires, ex: \"/organ/proj1/org1\")
   - Créer un projet : { \"type\": \"create_project\", \"id\": \"nom_variable_temp\", \"payload\": { \"title\": \"Titre du projet\", \"description\": \"Description...\", \"color\": \"#HEX_VIVANTE\", \"status\": \"ACTIVE\", \"iconType\": \"EMOJI\", \"iconData\": \"emoji_approprie\" } }
   - Créer un Organ : { \"type\": \"create_organ\", \"id\": \"nom_variable_temp\", \"projectUuid\": \"uuid_ou_variable_temp\", \"payload\": { \"title\": \"Titre de l'Organ\", \"description\": \"Description...\", \"highlightColor\": \"#HEX_VIVANTE\", \"iconType\": \"EMOJI\", \"iconData\": \"emoji_approprie\" } }
   - Créer un rôle d'Organ : { \"type\": \"create_role\", \"id\": \"nom_variable_temp\", \"projectUuid\": \"uuid_ou_variable_temp\", \"organUuid\": \"uuid_ou_variable_temp\", \"payload\": { \"name\": \"Nom du rôle\", \"iconType\": \"EMOJI\", \"iconData\": \"emoji_approprie\", \"permissions\": [\"PERM_1\", \"PERM_2\"] } }
   - Copier un rôle existant : { \"type\": \"copy_role\", \"projectUuid\": \"uuid_du_projet\", \"organUuid\": \"uuid_de_l_organ_source\", \"roleUuid\": \"uuid_du_role\" }
   - Coller le rôle précédemment copié : { \"type\": \"paste_role\", \"id\": \"nom_variable_temp\", \"projectUuid\": \"uuid_du_projet\", \"organUuid\": \"uuid_de_l_organ_cible\" }
   - Créer une tâche : { \"type\": \"create_task\", \"id\": \"nom_variable_temp\", \"projectUuid\": \"uuid_ou_variable_temp\", \"organUuid\": \"uuid_ou_variable_temp\", \"payload\": { \"title\": \"Titre de la tâche\", \"description\": \"Description...\", \"priority\": 1-10, \"status\": \"TODO\", \"estimatedHours\": \"4.5\", \"startDate\": \"2026-06-05T12:00:00\", \"expiresAt\": \"2026-06-12T18:00:00\", \"managerUuid\": \"uuid_du_responsable\", \"assigneeUuids\": [\"uuid_assigne\"], \"dependencyUuids\": [\"uuid_tache_ou_variable_temp\"], \"linkData\": [{\"url\": \"https://...\", \"description\": \"Description du lien\"}] } }
   - Modifier/Mettre à jour une tâche : { \"type\": \"update_task\", \"projectUuid\": \"uuid_ou_variable_temp\", \"organUuid\": \"uuid_ou_variable_temp\", \"taskUuid\": \"uuid\", \"payload\": { ... } }

    RÈGLES D'IDENTITÉ DES PROJETS / ORGANS / RÔLES :
    - Icône Emoji Obligatoire : Tout projet, Organ ou rôle créé DOIT avoir sa clé `iconType` définie à `\"EMOJI\"` et `iconData` contenant un emoji unique et pertinent (par exemple: 🚀 pour un projet tech, 📥 pour un backlog, 👑 pour un rôle de responsable). Ne laisse jamais ces champs vides ou par défaut.
    - Couleur Vibrante Obligatoire : Les projets et Organs créés DOIVENT avoir une couleur (`color` pour les projets, `highlightColor` pour les Organs) spécifiée sous forme de code HEX vif et vibrant (ex: `#FF7DD4`, `#355EE4`, `#10B981`, `#EF4444`, `#F59E0B`, `#8B5CF6`). Évite ABSOLUMENT le noir (`#000000`, `#000`) et les couleurs trop sombres ou neutres.

    RÈGLES DE CRÉATION DE RÔLES & PERMISSIONS :
    Tu as la possibilité de créer des rôles spécifiques dans les Organs via `create_role`. Un rôle est défini par son nom, son emoji, et un tableau de permissions.
    - RÈGLE DE COPIE & SIMILITUDE DE RÔLES (CRITIQUE) : Si l'utilisateur te demande de dupliquer ou de créer des rôles similaires à des rôles déjà existants dans d'autres Organs, tu dois impérativement générer une séquence d'actions combinant `copy_role` (sur l'Organ source) et `paste_role` (sur l'Organ cible). N'essaie pas de recréer manuellement le rôle à l'aide de `create_role`.
    Voici la liste complète des permissions disponibles que tu peux attribuer à un rôle :
    - ORGAN_VIEW, ORGAN_MANAGE_ROLES, ORGAN_MANAGE_MEMBERS, TASK_CREATE, TASK_EDIT_OWN, TASK_EDIT_ALL, TASK_DELETE_OWN, TASK_DELETE_ALL, TASK_STATUS_CHANGE_OWN, TASK_STATUS_CHANGE_ALL, TASK_PRIORITY_CHANGE_OWN, TASK_PRIORITY_CHANGE_ALL, TASK_DATES_MANAGE_OWN, TASK_DATES_MANAGE_ALL, TASK_ESTIMATE_MANAGE_OWN, TASK_ESTIMATE_MANAGE_ALL, TASK_ASSIGN_SELF, TASK_ASSIGN_ALL, TASK_LINK_MANAGE_OWN, TASK_LINK_MANAGE_ALL, TASK_TAG_MANAGE_OWN, TASK_TAG_MANAGE_ALL, TASK_DEPENDENCY_MANAGE_OWN, TASK_DEPENDENCY_MANAGE_ALL, COMMENT_CREATE, COMMENT_EDIT_OWN, COMMENT_EDIT_ALL, COMMENT_DELETE_OWN, COMMENT_DELETE_ALL, ATTACHMENT_ADD, ATTACHMENT_DELETE_OWN, ATTACHMENT_DELETE_ALL.

    RÈGLES DE CRÉATION DE TÂCHES :
     Sois extrêmement précis et exhaustif. Remplis tous les champs spécifiés ou induits par l'utilisateur :
     - Titre et Description : Rédige des titres clairs et des descriptions soignées. Ne mets pas de markdown complexe dans les valeurs du payload.
     - Priority : Un entier de 1 (faible) à 10 (critique).
     - Dates : Remplis `startDate` et `expiresAt` au format ISO (ex: \"2026-06-05T12:00:00\").
     - Responsable et assignés : Si l'utilisateur donne un responsable ou si c'est pertinent, choisis l'un des UUID de membres fournis dans la liste des membres ou l'UUID de l'utilisateur connecté actuel, et affecte-le à `managerUuid` et/ou dans le tableau `assigneeUuids`.
     - Dépendances : Si une tâche dépend d'une autre tâche créée dans la même séquence d'actions, réutilise sa variable temporaire ID (ex: `\"dependencyUuids\": [\"task1\"]`).

     RÈGLES DE CONCISION (CRITIQUE - Pour éviter le dépassement de limite et la troncature des réponses) :
     - Message : Limite ta réponse textuelle dans la clé \"message\" à 1 ou 2 phrases courtes maximum. Ne sois pas trop bavard.
     - Nombre de tâches : Limite-toi à un maximum de 3 ou 4 tâches créées dans la liste des actions (sauf si l'utilisateur demande explicitement un nombre supérieur précis de tâches).
     - Description des tâches : La description de chaque tâche dans le payload doit être très courte, d'une seule phrase simple (ex: \"Développer l'API REST.\").
     - Pièces jointes / Liens (linkData) : N'ajoute AUCUN lien (omet le champ `linkData`) sauf si l'utilisateur l'a explicitement demandé.

     EXÉCUTION EN ARRIÈRE-PLAN DIRECTE ET FLUX ININTERROMPU (CRITIQUE) :
     Toutes les créations doivent se faire de manière totalement automatisée par le frontend en tâche de fond. Tu ne dois JAMAIS renvoyer d'action avec un payload vide `{}`. Remplis systématiquement toutes les données détaillées directement dans la clé `payload`.

     DÉTAILS COMPLETS DE TOUTES LES INSTRUCTIONS (CRITIQUE) :
     La série d'actions que tu retournes dans le JSON doit être aussi complète que la demande de l'utilisateur (en respectant les règles de concision). Ne résume pas les étapes, génère toutes les actions de création et navigation requises. Si l'utilisateur demande de créer 5 tâches, tu DOIS générer l'intégralité des actions associées avec leurs détails respectifs. Ne résume pas, n'omet rien.

    NAVIGATION ET CONTRÔLE DE FIN :
    À la fin de toute séquence de création, tu dois impérativement inclure une action finale de type `navigate` vers le projet ou l'Organ nouvellement créé.

    VARIABLES TEMPORAIRES / CHAINAGE :
    Utilise la clé \"id\" de l'action de création pour définir une variable temporaire (ex: \"proj1\", \"org1\", \"task1\"), et réutiliser cette variable exacte dans les champs \"projectUuid\", \"organUuid\", \"dependencyUuids\" ou dans le chemin de navigation des actions suivantes.

    IMPORTANT :
    - La suppression (delete) de tout élément (projet, Organ, tâche) est strictement interdite pour l'IA. Ne propose jamais d'action de suppression.
    - Ne mentionne JAMAIS d'UUID brut ou de nom de variable temporaire (ex: 'proj1') dans la clé \"message\" (ex: '7b8c9d0e...'), sauf si l'utilisateur te le demande explicitement. Parle uniquement avec des noms conviviaux (ex: 'projet Marketing', 'Organ Tâches').
    - Ne mentionne JAMAIS de constantes de permission techniques (comme ORGAN_VIEW, TASK_CREATE, TASK_EDIT_OWN, COMMENT_CREATE, ATTACHMENT_ADD, etc.) dans la clé \"message\" destinée à l'utilisateur. Si l'utilisateur te pose des questions sur les permissions ou sur la configuration d'un rôle, traduis-les et explique-les toujours de manière naturelle, claire et vulgarisée en français (ex: « droit de voir l'Organ », « droit de modifier ses propres tâches », « droit de gérer les membres », « droit de poster des commentaires », « droit d'ajouter des pièces jointes »). Les constantes techniques en MAJUSCULES ne doivent apparaître QUE dans le tableau d'actions JSON, jamais dans tes phrases textuelles destinées à l'utilisateur.
    - Utilise toujours les UUID réels fournis dans les données courantes de l'utilisateur si l'élément existe déjà.
    - BUDGET DE TOKENS STRICT (CRITIQUE) : Tu as un budget de 1024 tokens maximum en sortie. Gère la taille de ton message et réduis le nombre d'actions au strict nécessaire pour que la réponse JSON soit complète, valide et entièrement fermée avant d'atteindre cette limite.";

        // Directives spécifiques à la commande slash
        $commandPrompt = "";
        $actionsEnabled = $context['actionsEnabled'] ?? true;

        if (!$actionsEnabled) {
            $commandPrompt = "\n\nATTENTION : L'utilisateur a désactivé l'autorisation d'exécuter des actions (mode Lecture Seule). Tu ne dois générer AUCUNE action de création, de modification ou de copie-coller (le tableau \"actions\" doit obligatoirement être vide []). Dans ton message textuel destiné à l'utilisateur, tu dois lui expliquer poliment que tu ne peux pas réaliser l'action demandée car les actions automatiques sont actuellement désactivées (il peut les réactiver via le bouton de réglage situé en bas du panneau). Explique-lui ensuite de manière pédagogique comment il peut réaliser cette action lui-même ou invite-le simplement à réactiver les actions.";
        } else {
            if ($command === 'create') {
                $commandPrompt = "\n\nEXIGENCE ABSOLUE SUR LA COMMANDE /create : L'utilisateur souhaite créer des éléments (projet, Organ, rôle, tâche). Le tableau \"actions\" ne doit JAMAIS être vide []. Tu DOIS impérativement y insérer toutes les actions de création (create_project, create_organ, create_role, create_task) nécessaires. Sois extrêmement exhaustif et ne t'arrête pas à mi-chemin. Si l'utilisateur n'a pas spécifié tous les détails, utilise ton imagination pour concevoir des éléments complets.";
            } elseif ($command === 'edit') {
                $commandPrompt = "\n\nEXIGENCE ABSOLUE SUR LA COMMANDE /edit : L'utilisateur souhaite modifier des éléments (statut, priorité, etc.). Le tableau \"actions\" ne doit JAMAIS être vide []. Tu DOIS impérativement y insérer les actions de modification (update_task) nécessaires.";
            } elseif ($command === 'role') {
                $commandPrompt = "\n\nEXIGENCE ABSOLUE SUR LA COMMANDE /role : L'utilisateur souhaite créer ou modifier un rôle. Le tableau \"actions\" ne doit JAMAIS être vide []. Tu DOIS impérativement y générer une action de type 'create_role' avec les permissions adéquates.";
            } else {
                $commandPrompt = "\n\nEXIGENCE SUR LA COMMANDE /ask (Mode par défaut) : Réponds de manière pédagogique en expliquant pas-à-pas à l'utilisateur comment IL peut effectuer l'action demandée. Ne dis surtout pas « je vais... » ou « je m'occupe de... » car c'est l'utilisateur qui effectuera les manipulations. Le tableau 'actions' de ta réponse doit être vide [] sauf si l'utilisateur demande explicitement d'aller sur une page (auquel cas tu mets une action 'navigate').";
            }
        }

        $systemPrompt .= $commandPrompt;

        // 6. Structurer l'historique des messages
        $messages = [
            ['role' => 'system', 'content' => $systemPrompt]
        ];

        $historySlice = array_slice($history, -10);
        foreach ($historySlice as $msg) {
            $role = ($msg['role'] === 'user' || $msg['role'] === 'system' || $msg['role'] === 'assistant') ? $msg['role'] : 'user';
            $messages[] = [
                'role' => $role,
                'content' => $msg['content'] ?? ''
            ];
        }

        // 7. Envoyer la requête à Nvidia NIM
        $payload = [
            'model' => $model,
            'messages' => $messages,
            'temperature' => 0.2,
            'max_tokens' => 1024,
            'response_format' => ['type' => 'json_object']
        ];

        $response = $this->callNvidiaApi($payload, $apiKey);

        if (isset($response['error_message'])) {
            return $this->json(['message' => $response['error_message'], 'actions' => []], Response::HTTP_BAD_GATEWAY);
        }

        $choice = $response['choices'][0] ?? [];
        $aiMessage = $choice['message'] ?? [];

        // 8. Décoder la réponse finale
        $aiRawContent = $aiMessage['content'] ?? '';
        $aiRawContent = trim($aiRawContent);
        error_log("[Chatbot] Raw response content: " . $aiRawContent);

        $chatbotAnswer = null;

        // Tente d'extraire la partie JSON entre le premier '{' et le dernier '}'
        $firstBrace = strpos($aiRawContent, '{');
        $lastBrace = strrpos($aiRawContent, '}');
        if ($firstBrace !== false && $lastBrace !== false && $lastBrace > $firstBrace) {
            $jsonCandidate = substr($aiRawContent, $firstBrace, $lastBrace - $firstBrace + 1);
            
            // Tente de décoder
            $chatbotAnswer = json_decode($jsonCandidate, true);
            
            if (!$chatbotAnswer) {
                // Tenter d'échapper les retours à la ligne illégaux dans les chaînes JSON pour réparer le format
                $repairedJson = preg_replace_callback('/"([^"\\\\]*(?:\\\\.[^"\\\\]*)*)"/s', function($matches) {
                    return '"' . str_replace(["\n", "\r"], ['\n', '\r'], $matches[1]) . '"';
                }, $jsonCandidate);
                $chatbotAnswer = json_decode($repairedJson, true);
            }
        }

        // Si la réponse n'est pas un tableau valide ou n'a pas de clé message
        if (!$chatbotAnswer || !is_array($chatbotAnswer) || !isset($chatbotAnswer['message'])) {
            $chatbotAnswer = $this->repairTruncatedJson($aiRawContent);
        }

        // Assurer que la clé 'actions' existe toujours
        if (!isset($chatbotAnswer['actions']) || !is_array($chatbotAnswer['actions'])) {
            $chatbotAnswer['actions'] = [];
        }

        return $this->json($chatbotAnswer);
    }

    /**
     * Tente de récupérer le message textuel d'un JSON malformé ou tronqué.
     */
    private function extractMessageFromMalformedJson(string $rawContent): ?string
    {
        // 1. Essai de match exact de la valeur de "message"
        if (preg_match('/"message"\s*:\s*"((?:[^"\\\\]|\\\\.)*)"/s', $rawContent, $matches)) {
            return stripcslashes($matches[1]);
        }

        // 2. Si le message est tronqué à la fin sans guillemet de fermeture
        if (preg_match('/"message"\s*:\s*"(.*)$/s', $rawContent, $matches)) {
            $content = $matches[1];
            // On essaie de couper tout ce qui ressemble à la suite ("actions"...)
            $content = preg_replace('/"\s*,\s*"actions"\s*:\s*.*$/s', '', $content);
            $content = rtrim($content, "\r\n\t }\"");
            return stripcslashes($content);
        }

        return null;
    }

    /**
     * Tente de réparer un JSON d'actions de l'assistant virtuel tronqué
     * en extrayant tous les objets d'action complets.
     */
    private function repairTruncatedJson(string $rawContent): ?array
    {
        $actionsStartPos = strpos($rawContent, '"actions"');
        if ($actionsStartPos === false) {
            return null;
        }

        $bracketStart = strpos($rawContent, '[', $actionsStartPos);
        if ($bracketStart === false) {
            return null;
        }

        $actionsStr = substr($rawContent, $bracketStart);
        $len = strlen($actionsStr);

        $actions = [];
        $nestingLevel = 0;
        $inString = false;
        $escape = false;
        $currentActionStart = -1;

        for ($i = 0; $i < $len; $i++) {
            $char = $actionsStr[$i];
            if ($escape) {
                $escape = false;
                continue;
            }
            if ($char === '\\') {
                $escape = true;
                continue;
            }
            if ($char === '"') {
                $inString = !$inString;
                continue;
            }

            if (!$inString) {
                if ($char === '{') {
                    if ($nestingLevel === 0) {
                        $currentActionStart = $i;
                    }
                    $nestingLevel++;
                } elseif ($char === '}') {
                    if ($nestingLevel > 0) {
                        $nestingLevel--;
                        if ($nestingLevel === 0 && $currentActionStart !== -1) {
                            $actionJson = substr($actionsStr, $currentActionStart, $i - $currentActionStart + 1);
                            $decodedAction = json_decode($actionJson, true);
                            if (is_array($decodedAction)) {
                                $actions[] = $decodedAction;
                            }
                            $currentActionStart = -1;
                        }
                    }
                }
            }
        }

        $message = $this->extractMessageFromMalformedJson($rawContent);

        return [
            'message' => $message ?? "Désolé, la réponse de l'assistant a été tronquée.",
            'actions' => $actions
        ];
    }

    /**
     * Exécute un appel HTTP vers Nvidia NIM
     */
    private function callNvidiaApi(array $payload, string $apiKey): array
    {
        $ch = curl_init('https://integrate.api.nvidia.com/v1/chat/completions');
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload));
        curl_setopt($ch, CURLOPT_HTTPHEADER, [
            'Authorization: Bearer ' . $apiKey,
            'Content-Type: application/json'
        ]);
        curl_setopt($ch, CURLOPT_TIMEOUT, 360);
        curl_setopt($ch, CURLOPT_IPRESOLVE, CURL_IPRESOLVE_V4);

        $response = curl_exec($ch);
        $err = curl_error($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        if ($err) {
            return ['error_message' => "Impossible de contacter l'assistant virtuel (Erreur de réseau : " . $err . ")."];
        }

        if ($httpCode !== 200) {
            return ['error_message' => "L'assistant virtuel a retourné une réponse invalide (Code HTTP " . $httpCode . ")."];
        }

        return json_decode((string)$response, true) ?? [];
    }
}
