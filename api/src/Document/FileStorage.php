<?php

namespace App\Document;

use Doctrine\ODM\MongoDB\Mapping\Annotations as MongoDB;

#[MongoDB\Document(collection: 'file_storage')]
class FileStorage
{
    #[MongoDB\Id]
    private ?string $id = null;

    #[MongoDB\Field(type: 'string')]
    private ?string $taskAttachmentUuid = null;

    #[MongoDB\Field(type: 'string')]
    private ?string $filename = null;

    #[MongoDB\Field(type: 'string')]
    private ?string $mimeType = null;

    #[MongoDB\Field(type: 'int')]
    private ?int $size = null;

    #[MongoDB\Field(type: 'string')]
    private ?string $checksum = null;

    #[MongoDB\Field(type: 'bin')]
    private $content = null;

    #[MongoDB\Field(type: 'hash')]
    private ?array $metadata = null;

    #[MongoDB\Field(type: 'date')]
    private ?\DateTimeInterface $uploadedAt = null;

    #[MongoDB\Field(type: 'int')]
    private int $version = 1;

    public function __construct()
    {
        $this->uploadedAt = new \DateTime();
    }

    public function getId(): ?string
    {
        return $this->id;
    }

    public function getTaskAttachmentUuid(): ?string
    {
        return $this->taskAttachmentUuid;
    }

    public function setTaskAttachmentUuid(?string $taskAttachmentUuid): static
    {
        $this->taskAttachmentUuid = $taskAttachmentUuid;
        return $this;
    }

    public function getFilename(): ?string
    {
        return $this->filename;
    }

    public function setFilename(?string $filename): static
    {
        $this->filename = $filename;
        return $this;
    }

    public function getMimeType(): ?string
    {
        return $this->mimeType;
    }

    public function setMimeType(?string $mimeType): static
    {
        $this->mimeType = $mimeType;
        return $this;
    }

    public function getSize(): ?int
    {
        return $this->size;
    }

    public function setSize(?int $size): static
    {
        $this->size = $size;
        return $this;
    }

    public function getChecksum(): ?string
    {
        return $this->checksum;
    }

    public function setChecksum(?string $checksum): static
    {
        $this->checksum = $checksum;
        return $this;
    }

    public function getContent()
    {
        return $this->content;
    }

    public function setContent($content): static
    {
        $this->content = $content;
        return $this;
    }

    public function getMetadata(): ?array
    {
        return $this->metadata;
    }

    public function setMetadata(?array $metadata): static
    {
        $this->metadata = $metadata;
        return $this;
    }

    public function getUploadedAt(): ?\DateTimeInterface
    {
        return $this->uploadedAt;
    }

    public function setUploadedAt(?\DateTimeInterface $uploadedAt): static
    {
        $this->uploadedAt = $uploadedAt;
        return $this;
    }

    public function getVersion(): int
    {
        return $this->version;
    }

    public function setVersion(int $version): static
    {
        $this->version = $version;
        return $this;
    }
}
