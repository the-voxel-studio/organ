<?php

namespace App\Service;

use App\Document\FileStorage;
use Doctrine\ODM\MongoDB\DocumentManager;
use MongoDB\GridFS\Bucket;
use Symfony\Component\HttpFoundation\File\UploadedFile;

class MongoFileStorageService
{
    private Bucket $bucket;

    public function __construct(
        private DocumentManager $dm
    ) {
        $this->bucket = $this->dm->getClient()->selectDatabase($this->dm->getConfiguration()->getDefaultDB())->selectGridFSBucket();
    }

    public function store(UploadedFile $file, string $taskAttachmentUuid): string
    {
        if ($file->getSize() > 20 * 1024 * 1024) {
            throw new \Exception('File size exceeds 20MB limit for MongoDB storage.');
        }

        $checksum = hash_file('sha256', $file->getRealPath());

        // Deduplication: check if a file with the same checksum already exists in GridFS
        try {
            $existingFile = $this->dm->getClient()
                ->selectDatabase($this->dm->getConfiguration()->getDefaultDB())
                ->selectCollection('fs.files')
                ->findOne(['metadata.checksum' => $checksum]);

            if ($existingFile) {
                return (string) $existingFile['_id'];
            }
        } catch (\Exception $e) {
            // Fallback to uploading if database query fails
        }

        $stream = fopen($file->getRealPath(), 'rb');
        $id = $this->bucket->uploadFromStream($file->getClientOriginalName(), $stream, [
            'metadata' => [
                'taskAttachmentUuid' => $taskAttachmentUuid,
                'mimeType' => $file->getClientMimeType(),
                'size' => $file->getSize(),
                'checksum' => $checksum,
                'uploadedAt' => new \DateTime(),
                'version' => 1
            ]
        ]);
        fclose($stream);

        return (string) $id;
    }

    public function download(string $mongoFileId)
    {
        try {
            $stream = $this->bucket->openDownloadStream(new \MongoDB\BSON\ObjectId($mongoFileId));
            return $stream;
        } catch (\Exception $e) {
            return null;
        }
    }

    public function delete(string $mongoFileId): void
    {
        try {
            $this->bucket->delete(new \MongoDB\BSON\ObjectId($mongoFileId));
        } catch (\Exception $e) {
            // Log or ignore if already deleted
        }
    }

    public function getMetadata(string $mongoFileId): ?array
    {
        try {
            $file = $this->dm->getClient()
                ->selectDatabase($this->dm->getConfiguration()->getDefaultDB())
                ->selectCollection('fs.files')
                ->findOne(['_id' => new \MongoDB\BSON\ObjectId($mongoFileId)]);
            
            return $file ? (array) $file['metadata'] : null;
        } catch (\Exception $e) {
            return null;
        }
    }
}
