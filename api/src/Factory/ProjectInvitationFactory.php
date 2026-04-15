<?php

namespace App\Factory;

use App\Entity\ProjectInvitation;
use App\Enum\ProjectGlobalRole;
use Zenstruck\Foundry\Persistence\PersistentObjectFactory;

/**
 * @extends PersistentObjectFactory<ProjectInvitation>
 */
final class ProjectInvitationFactory extends PersistentObjectFactory
{
    /**
     * @see https://symfony.com/bundles/ZenstruckFoundryBundle/current/index.html#factories-as-services
     *
     * @todo inject services if required
     */
    public function __construct()
    {
    }

    #[\Override]
    public static function class(): string
    {
        return ProjectInvitation::class;
    }

    /**
     * @see https://symfony.com/bundles/ZenstruckFoundryBundle/current/index.html#model-factories
     *
     * @todo add your default values here
     */
    #[\Override]
    protected function defaults(): array|callable
    {
        return [
            'email' => self::faker()->text(180),
            'expiresAt' => self::faker()->dateTime(),
            'project' => ProjectFactory::new(),
            'role' => self::faker()->randomElement(ProjectGlobalRole::cases()),
        ];
    }

    /**
     * @see https://symfony.com/bundles/ZenstruckFoundryBundle/current/index.html#initialization
     */
    #[\Override]
    protected function initialize(): static
    {
        return $this
            // ->afterInstantiate(function(ProjectInvitation $projectInvitation): void {})
        ;
    }
}
