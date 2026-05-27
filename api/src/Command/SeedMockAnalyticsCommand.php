<?php

namespace App\Command;

use App\Document\AuditLog;
use App\Document\DailyStat;
use App\Entity\Project;
use App\Entity\Organ;
use App\Entity\Task;
use App\Entity\User;
use App\Service\StatsAggregationService;
use Doctrine\ODM\MongoDB\DocumentManager;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Component\Console\Attribute\AsCommand;
use Symfony\Component\Console\Command\Command;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;
use Symfony\Component\Console\Style\SymfonyStyle;

#[AsCommand(
    name: 'app:seed:mock-analytics',
    description: 'Generates a week of realistic mock audit logs and aggregated stats for seeded projects.',
)]
class SeedMockAnalyticsCommand extends Command
{
    public function __construct(
        private EntityManagerInterface $em,
        private DocumentManager $dm,
        private StatsAggregationService $statsAggregationService
    ) {
        parent::__construct();
    }

    protected function execute(InputInterface $input, OutputInterface $output): int
    {
        $io = new SymfonyStyle($input, $output);
        $io->title('Seeding Mock Analytics and Audit Logs (7-day simulation)');

        // 1. Clear des anciennes données MongoDB
        $io->text('Clearing existing audit logs and daily stats from MongoDB...');
        $this->dm->getDocumentCollection(AuditLog::class)->deleteMany([]);
        $this->dm->getDocumentCollection(DailyStat::class)->deleteMany([]);

        // 2. Récupère les objets MySQL
        $projects = $this->em->getRepository(Project::class)->findAll();
        if (empty($projects)) {
            $io->error('No projects found in MySQL. Please run seed.sql first.');
            return Command::FAILURE;
        }

        $users = $this->em->getRepository(User::class)->findAll();
        if (empty($users)) {
            $io->error('No users found in MySQL.');
            return Command::FAILURE;
        }

        $actions = [
            'CREATE', 'STATUS_CHANGE', 'COMMENT_ADD', 'ATTACHMENT_ADD', 'UPDATE', 'CONSULTATION'
        ];

        $fields = ['title', 'description', 'dueDate', 'priority'];

        $now = new \DateTime();

        // 3. Boucle sur les 7 derniers jours
        for ($dayOffset = 7; $dayOffset >= 0; $dayOffset--) {
            $currentSimulatedDate = (clone $now)->modify("-{$dayOffset} days");
            $currentSimulatedDate->setTime(0, 0, 0);
            
            $io->text(sprintf('Simulating logs for %s...', $currentSimulatedDate->format('Y-m-d')));

            // Nombre de logs aléatoire pour ce jour (entre 15 et 30)
            $logCount = rand(15, 30);
            
            for ($i = 0; $i < $logCount; $i++) {
                // Sélection aléatoire projet, user, action
                /** @var Project $project */
                $project = $projects[array_rand($projects)];
                /** @var User $user */
                $user = $users[array_rand($users)];

                // Récupère les organes via repository
                $organs = $this->em->getRepository(Organ::class)->findBy(['project' => $project]);
                $organ = !empty($organs) ? $organs[array_rand($organs)] : null;

                // Récupère les tâches via repository
                $tasks = $organ ? $this->em->getRepository(Task::class)->findBy(['organ' => $organ]) : [];
                $task = !empty($tasks) ? $tasks[array_rand($tasks)] : null;

                $actionType = $actions[array_rand($actions)];

                // Heure aléatoire de la journée
                $logTime = clone $currentSimulatedDate;
                $logTime->setTime(rand(8, 20), rand(0, 59), rand(0, 59));

                $auditLog = new AuditLog();
                $auditLog->setProjectUuid($project->getUuid());
                $auditLog->setUserUuid($user->getUuid());
                $auditLog->setActionType($actionType);
                $auditLog->setCreatedAt($logTime);
                $auditLog->setContext([
                    'ip' => '192.168.1.' . rand(2, 254),
                    'method' => in_array($actionType, ['CREATE', 'COMMENT_ADD', 'ATTACHMENT_ADD']) ? 'POST' : (in_array($actionType, ['UPDATE', 'STATUS_CHANGE']) ? 'PATCH' : 'GET')
                ]);

                if ($organ) {
                    $auditLog->setOrganUuid($organ->getUuid());
                }

                if ($task) {
                    $auditLog->setTaskUuid($task->getUuid());
                }

                // Valeurs spécifiques selon l'action
                switch ($actionType) {
                    case 'CREATE':
                        $auditLog->setFieldName(null);
                        $auditLog->setOldValues(null);
                        $auditLog->setNewValues([
                            'title' => $task ? $task->getTitle() : 'Nouvelle tâche',
                            'status' => 'TODO'
                        ]);
                        break;

                    case 'STATUS_CHANGE':
                        $statuses = ['TODO', 'IN_PROGRESS', 'WAITING', 'DONE', 'CANCELED'];
                        $oldStatus = $statuses[array_rand($statuses)];
                        $newStatus = $statuses[array_rand($statuses)];
                        while ($oldStatus === $newStatus) {
                            $newStatus = $statuses[array_rand($statuses)];
                        }
                        $auditLog->setFieldName('status');
                        $auditLog->setOldValues(['status' => $oldStatus]);
                        $auditLog->setNewValues(['status' => $newStatus]);
                        break;

                    case 'COMMENT_ADD':
                        $auditLog->setFieldName(null);
                        $auditLog->setOldValues(null);
                        $auditLog->setNewValues([
                            'content' => 'Super travail !' . (rand(0, 1) ? ' On avance bien.' : '')
                        ]);
                        break;

                    case 'ATTACHMENT_ADD':
                        $auditLog->setFieldName(null);
                        $auditLog->setOldValues(null);
                        $auditLog->setNewValues([
                            'fileName' => 'document_' . rand(1, 100) . '.pdf',
                            'size' => rand(100, 5000) * 1024
                        ]);
                        break;

                    case 'UPDATE':
                        $field = $fields[array_rand($fields)];
                        $auditLog->setFieldName($field);
                        if ($field === 'priority') {
                            $oldVal = rand(1, 5);
                            $newVal = rand(6, 10);
                            $auditLog->setOldValues(['priority' => $oldVal]);
                            $auditLog->setNewValues(['priority' => $newVal]);
                        } elseif ($field === 'dueDate') {
                            $oldVal = (clone $logTime)->modify('+' . rand(1, 5) . ' days')->format('Y-m-d');
                            $newVal = (clone $logTime)->modify('+' . rand(6, 12) . ' days')->format('Y-m-d');
                            $auditLog->setOldValues(['dueDate' => $oldVal]);
                            $auditLog->setNewValues(['dueDate' => $newVal]);
                        } else {
                            $auditLog->setOldValues([$field => 'Ancienne valeur']);
                            $auditLog->setNewValues([$field => 'Nouvelle valeur']);
                        }
                        break;

                    case 'CONSULTATION':
                        // Pas de champs ou valeurs pour les consultations
                        $auditLog->setFieldName(null);
                        $auditLog->setOldValues(null);
                        $auditLog->setNewValues(null);
                        break;
                }

                $this->dm->persist($auditLog);
            }

            $this->dm->flush();

            // 4. Agrégation des stats pour ce jour
            $this->statsAggregationService->aggregateForDate($currentSimulatedDate);
        }

        $io->success('Mock analytics data and statistics generated successfully for the past 7 days!');
        return Command::SUCCESS;
    }
}
