<?php

namespace App\Document;

use Doctrine\ODM\MongoDB\Mapping\Annotations as MongoDB;

#[MongoDB\Document(collection: 'audit_logs')]
#[MongoDB\Index(keys: ['taskUuid' => 1, 'createdAt' => -1])]
#[MongoDB\Index(keys: ['organUuid' => 1, 'createdAt' => -1])]
#[MongoDB\Index(keys: ['projectUuid' => 1, 'createdAt' => -1])]
#[MongoDB\Index(keys: ['userUuid' => 1])]
class AuditLog
{
    #[MongoDB\Id]
    private ?string $id = null;

    #[MongoDB\Field(type: 'string')]
    private ?string $taskUuid = null;

    #[MongoDB\Field(type: 'string')]
    private ?string $organUuid = null;

    #[MongoDB\Field(type: 'string')]
    private ?string $projectUuid = null;

    #[MongoDB\Field(type: 'string')]
    private ?string $userUuid = null;

    #[MongoDB\Field(type: 'string')]
    private ?string $actionType = null;

    #[MongoDB\Field(type: 'string')]
    private ?string $fieldName = null;

    #[MongoDB\Field(type: 'hash')]
    private ?array $oldValues = null;

    #[MongoDB\Field(type: 'hash')]
    private ?array $newValues = null;

    #[MongoDB\Field(type: 'hash')]
    private ?array $context = null;

    #[MongoDB\Field(type: 'date')]
    private ?\DateTimeInterface $createdAt = null;

    public function __construct()
    {
        $this->createdAt = new \DateTime();
    }

    public function getId(): ?string
    {
        return $this->id;
    }

    public function getTaskUuid(): ?string
    {
        return $this->taskUuid;
    }

    public function setTaskUuid(?string $taskUuid): static
    {
        $this->taskUuid = $taskUuid;
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

    public function getProjectUuid(): ?string
    {
        return $this->projectUuid;
    }

    public function setProjectUuid(?string $projectUuid): static
    {
        $this->projectUuid = $projectUuid;
        return $this;
    }

    public function getUserUuid(): ?string
    {
        return $this->userUuid;
    }

    public function setUserUuid(?string $userUuid): static
    {
        $this->userUuid = $userUuid;
        return $this;
    }

    public function getActionType(): ?string
    {
        return $this->actionType;
    }

    public function setActionType(?string $actionType): static
    {
        $this->actionType = $actionType;
        return $this;
    }

    public function getFieldName(): ?string
    {
        return $this->fieldName;
    }

    public function setFieldName(?string $fieldName): static
    {
        $this->fieldName = $fieldName;
        return $this;
    }

    public function getOldValues(): ?array
    {
        return $this->oldValues;
    }

    public function setOldValues(?array $oldValues): static
    {
        $this->oldValues = $oldValues;
        return $this;
    }

    public function getNewValues(): ?array
    {
        return $this->newValues;
    }

    public function setNewValues(?array $newValues): static
    {
        $this->newValues = $newValues;
        return $this;
    }

    public function getContext(): ?array
    {
        return $this->context;
    }

    public function setContext(?array $context): static
    {
        $this->context = $context;
        return $this;
    }

    public function getCreatedAt(): ?\DateTimeInterface
    {
        return $this->createdAt;
    }

    public function setCreatedAt(?\DateTimeInterface $createdAt): static
    {
        $this->createdAt = $createdAt;
        return $this;
    }
}
