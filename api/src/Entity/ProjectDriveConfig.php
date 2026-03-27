<?php

declare(strict_types=1);

namespace App\Entity;

use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Uid\Uuid;

#[ORM\Entity]
#[ORM\Table(name: 'project_drive_configs')]
class ProjectDriveConfig
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    private ?int $id = null;

    #[ORM\Column(length: 36, unique: true)]
    private string $uuid;

    #[ORM\OneToOne(targetEntity: Project::class)]
    #[ORM\JoinColumn(nullable: false, unique: true, onDelete: 'CASCADE')]
    private ?Project $project = null;

    #[ORM\Column(length: 255)]
    private ?string $driveFolderId = null;

    #[ORM\Column(type: Types::TEXT)]
    private ?string $encryptedRefreshToken = null;

    #[ORM\Column(options: ['default' => 1])]
    private bool $isActive = true;

    public function __construct()
    {
        $this->uuid = Uuid::v4()->toRfc4122();
    }

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getUuid(): string
    {
        return $this->uuid;
    }

    public function setUuid(string $uuid): static
    {
        $this->uuid = $uuid;
        return $this;
    }

    public function getProject(): ?Project
    {
        return $this->project;
    }

    public function setProject(Project $project): static
    {
        $this->project = $project;
        return $this;
    }

    public function getDriveFolderId(): ?string
    {
        return $this->driveFolderId;
    }

    public function setDriveFolderId(string $driveFolderId): static
    {
        $this->driveFolderId = $driveFolderId;
        return $this;
    }

    public function getEncryptedRefreshToken(): ?string
    {
        return $this->encryptedRefreshToken;
    }

    public function setEncryptedRefreshToken(string $encryptedRefreshToken): static
    {
        $this->encryptedRefreshToken = $encryptedRefreshToken;
        return $this;
    }

    public function isActive(): bool
    {
        return $this->isActive;
    }

    public function setIsActive(bool $isActive): static
    {
        $this->isActive = $isActive;
        return $this;
    }
}
