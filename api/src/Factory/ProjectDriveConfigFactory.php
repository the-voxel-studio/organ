<?php

namespace App\Factory;

use App\Entity\ProjectDriveConfig;
use Zenstruck\Foundry\Persistence\PersistentObjectFactory;

/**
 * @extends PersistentObjectFactory<ProjectDriveConfig>
 */
final class ProjectDriveConfigFactory extends PersistentObjectFactory
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
        return ProjectDriveConfig::class;
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
            'driveFolderId' => self::faker()->text(255),
            'encryptedRefreshToken' => self::faker()->text(),
            'isActive' => self::faker()->boolean(),
            'project' => ProjectFactory::new(),
            'uuid' => self::faker()->text(36),
        ];
    }

    /**
     * @see https://symfony.com/bundles/ZenstruckFoundryBundle/current/index.html#initialization
     */
    #[\Override]
    protected function initialize(): static
    {
        return $this
            // ->afterInstantiate(function(ProjectDriveConfig $projectDriveConfig): void {})
        ;
    }
}
