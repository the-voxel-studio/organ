<?php

namespace App\Document;

use Doctrine\ODM\MongoDB\Mapping\Annotations as MongoDB;

#[MongoDB\Document(collection: 'daily_stats')]
#[MongoDB\UniqueIndex(keys: ['date' => 1, 'projectUuid' => 1, 'organUuid' => 1])]
class DailyStat
{
    #[MongoDB\Id]
    private ?string $id = null;

    #[MongoDB\Field(type: 'date')]
    private ?\DateTimeInterface $date = null;

    #[MongoDB\Field(type: 'string')]
    private ?string $projectUuid = null;

    #[MongoDB\Field(type: 'string')]
    private ?string $organUuid = null;

    #[MongoDB\Field(type: 'int')]
    private int $tasksCreated = 0;

    #[MongoDB\Field(type: 'int')]
    private int $tasksCompleted = 0;

    #[MongoDB\Field(type: 'int')]
    private int $tasksCanceled = 0;

    #[MongoDB\Field(type: 'int')]
    private int $commentsAdded = 0;

    #[MongoDB\Field(type: 'int')]
    private int $attachmentsAdded = 0;

    #[MongoDB\Field(type: 'int')]
    private int $attachmentsSize = 0;

    #[MongoDB\Field(type: 'int')]
    private int $membersActive = 0;

    #[MongoDB\Field(type: 'hash')]
    private array $statusChanges = [];

    #[MongoDB\Field(type: 'hash')]
    private array $extra = [];

    public function getId(): ?string
    {
        return $this->id;
    }

    public function getDate(): ?\DateTimeInterface
    {
        return $this->date;
    }

    public function setDate(?\DateTimeInterface $date): static
    {
        $this->date = $date;
        return $this;
    }

    public function getProjectUuid(): ?string
    {
        return $this->projectUuid;
    }

    public function setProjectUuid(?string $projectUuid): static
    {
        $this->projectUuid = $projectUuid;
        return $this;
    }

    public function getOrganUuid(): ?string
    {
        return $this->organUuid;
    }

    public function setOrganUuid(?string $organUuid): static
    {
        $this->organUuid = $organUuid;
        return $this;
    }

    public function getTasksCreated(): int
    {
        return $this->tasksCreated;
    }

    public function setTasksCreated(int $tasksCreated): static
    {
        $this->tasksCreated = $tasksCreated;
        return $this;
    }

    public function getTasksCompleted(): int
    {
        return $this->tasksCompleted;
    }

    public function setTasksCompleted(int $tasksCompleted): static
    {
        $this->tasksCompleted = $tasksCompleted;
        return $this;
    }

    public function getTasksCanceled(): int
    {
        return $this->tasksCanceled;
    }

    public function setTasksCanceled(int $tasksCanceled): static
    {
        $this->tasksCanceled = $tasksCanceled;
        return $this;
    }

    public function getCommentsAdded(): int
    {
        return $this->commentsAdded;
    }

    public function setCommentsAdded(int $commentsAdded): static
    {
        $this->commentsAdded = $commentsAdded;
        return $this;
    }

    public function getAttachmentsAdded(): int
    {
        return $this->attachmentsAdded;
    }

    public function setAttachmentsAdded(int $attachmentsAdded): static
    {
        $this->attachmentsAdded = $attachmentsAdded;
        return $this;
    }

    public function getAttachmentsSize(): int
    {
        return $this->attachmentsSize;
    }

    public function setAttachmentsSize(int $attachmentsSize): static
    {
        $this->attachmentsSize = $attachmentsSize;
        return $this;
    }

    public function getMembersActive(): int
    {
        return $this->membersActive;
    }

    public function setMembersActive(int $membersActive): static
    {
        $this->membersActive = $membersActive;
        return $this;
    }

    public function getStatusChanges(): array
    {
        return $this->statusChanges;
    }

    public function setStatusChanges(array $statusChanges): static
    {
        $this->statusChanges = $statusChanges;
        return $this;
    }

    public function getExtra(): array
    {
        return $this->extra;
    }

    public function setExtra(array $extra): static
    {
        $this->extra = $extra;
        return $this;
    }
}
