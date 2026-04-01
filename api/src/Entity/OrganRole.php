<?php

declare(strict_types=1);

namespace App\Entity;

use App\Enum\IconType;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Uid\Uuid;

#[ORM\Entity]
#[ORM\Table(name: 'organ_roles')]
#[ORM\Index(name: 'idx_organ', columns: ['organ_id'])]
class OrganRole
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    private ?int $id = null;

    #[ORM\Column(length: 36, unique: true)]
    private string $uuid;

    #[ORM\ManyToOne(targetEntity: Organ::class)]
    #[ORM\JoinColumn(nullable: false, onDelete: 'CASCADE')]
    private ?Organ $organ = null;

    #[ORM\Column(length: 50)]
    private ?string $name = null;

    #[ORM\Column(length: 255, enumType: IconType::class, options: ['default' => IconType::EMOJI, 'comment' => 'SVG, BLOB, EMOJI'])]
    private IconType $iconType = IconType::EMOJI;

    #[ORM\Column(type: Types::TEXT, nullable: true, columnDefinition: 'LONGTEXT')]
    private ?string $iconData = null;

    #[ORM\Column(type: Types::DATETIME_MUTABLE, nullable: true)]
    private ?\DateTimeInterface $deletedAt = null;

    /**
     * @var Collection<int, Permission>
     */
    #[ORM\ManyToMany(targetEntity: Permission::class)]
    #[ORM\JoinTable(name: 'organ_role_permissions')]
    #[ORM\JoinColumn(name: 'role_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    #[ORM\InverseJoinColumn(name: 'permission_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private Collection $permissions;

    public function __construct()
    {
        $this->uuid = Uuid::v4()->toRfc4122();
        $this->permissions = new ArrayCollection();
        $this->iconType = IconType::EMOJI;
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

    public function getOrgan(): ?Organ
    {
        return $this->organ;
    }

    public function setOrgan(?Organ $organ): static
    {
        $this->organ = $organ;
        return $this;
    }

    public function getName(): ?string
    {
        return $this->name;
    }

    public function setName(string $name): static
    {
        $this->name = $name;
        return $this;
    }

    public function getIconType(): IconType
    {
        return $this->iconType;
    }

    public function setIconType(IconType $iconType): static
    {
        $this->iconType = $iconType;
        return $this;
    }

    public function getIconData(): ?string
    {
        return $this->iconData;
    }

    public function setIconData(?string $iconData): static
    {
        $this->iconData = $iconData;
        return $this;
    }

    public function getDeletedAt(): ?\DateTimeInterface
    {
        return $this->deletedAt;
    }

    public function setDeletedAt(?\DateTimeInterface $deletedAt): static
    {
        $this->deletedAt = $deletedAt;
        return $this;
    }

    /**
     * @return Collection<int, Permission>
     */
    public function getPermissions(): Collection
    {
        return $this->permissions;
    }

    public function addPermission(Permission $permission): static
    {
        if (!$this->permissions->contains($permission)) {
            $this->permissions->add($permission);
        }

        return $this;
    }

    public function removePermission(Permission $permission): static
    {
        $this->permissions->removeElement($permission);

        return $this;
    }
}
