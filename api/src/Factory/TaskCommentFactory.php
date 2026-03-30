<?php

namespace App\Factory;

use App\Entity\TaskComment;
use Zenstruck\Foundry\Persistence\PersistentObjectFactory;

/**
 * @extends PersistentObjectFactory<TaskComment>
 */
final class TaskCommentFactory extends PersistentObjectFactory
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
        return TaskComment::class;
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
            'content' => self::faker()->text(),
            'createdAt' => self::faker()->dateTime(),
            'task' => TaskFactory::new(),
            'updatedAt' => self::faker()->dateTime(),
            'user' => UserFactory::new(),
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
            // ->afterInstantiate(function(TaskComment $taskComment): void {})
        ;
    }
}
