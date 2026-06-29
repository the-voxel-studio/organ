<?php

declare(strict_types=1);

namespace App\Tests\Controller;

use App\Entity\TaskAttachment;
use App\Enum\ProjectGlobalRole;
use App\Factory\OrganFactory;
use App\Factory\ProjectFactory;
use App\Factory\ProjectMemberFactory;
use App\Factory\TaskFactory;
use App\Factory\UserFactory;
use App\Service\MongoFileStorageService;
use App\Tests\ApiTestCase;
use Symfony\Component\HttpFoundation\File\UploadedFile;

class TaskAttachmentControllerTest extends ApiTestCase
{
    private string $tempFile1;
    private string $tempFile2;

    protected function setUp(): void
    {
        parent::setUp();

        // Use a valid 1x1 PNG image as base so the mime type detector identifies it as image/png
        $pngBase = base64_decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=');
        $content = $pngBase . '_' . uniqid();
        
        $this->tempFile1 = tempnam(sys_get_temp_dir(), 'test1_') . '.png';
        $this->tempFile2 = tempnam(sys_get_temp_dir(), 'test2_') . '.png';
        file_put_contents($this->tempFile1, $content);
        file_put_contents($this->tempFile2, $content);
    }

    protected function tearDown(): void
    {
        if (file_exists($this->tempFile1)) {
            unlink($this->tempFile1);
        }
        if (file_exists($this->tempFile2)) {
            unlink($this->tempFile2);
        }

        parent::tearDown();
    }

    public function testUploadDeduplicationAndSafeDeletion(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne();
        
        ProjectMemberFactory::createOne([
            'user' => $user,
            'project' => $project,
            'globalRole' => ProjectGlobalRole::ADMIN
        ]);
        
        $organ = OrganFactory::createOne(['project' => $project]);
        $task = TaskFactory::createOne(['organ' => $organ]);

        $this->login($client, $user);

        // 1. Upload first file
        $file1 = new UploadedFile(
            $this->tempFile1,
            'photo1.png',
            'image/png',
            null,
            true // test mode
        );

        $url = sprintf(
            '/api/projects/%s/organs/%s/tasks/%s/attachments',
            $project->getUuid(),
            $organ->getUuid(),
            $task->getUuid()
        );

        $client->request('POST', $url, [], ['file' => $file1]);
        $this->assertResponseStatusCodeSame(201);
        $data1 = $this->getResponseContent($client);
        $this->assertNotEmpty($data1['uuid']);

        // 2. Upload second identical file
        $file2 = new UploadedFile(
            $this->tempFile2,
            'photo2.png',
            'image/png',
            null,
            true // test mode
        );

        $client->request('POST', $url, [], ['file' => $file2]);
        $this->assertResponseStatusCodeSame(201);
        $data2 = $this->getResponseContent($client);
        $this->assertNotEmpty($data2['uuid']);

        // 3. Verify deduplication (both TaskAttachments must reference the SAME mongoFileId)
        $container = static::getContainer();
        $em = $container->get('doctrine.orm.entity_manager');
        
        /** @var TaskAttachment $att1 */
        $att1 = $em->getRepository(TaskAttachment::class)->findOneBy(['uuid' => $data1['uuid']]);
        /** @var TaskAttachment $att2 */
        $att2 = $em->getRepository(TaskAttachment::class)->findOneBy(['uuid' => $data2['uuid']]);

        $this->assertNotNull($att1);
        $this->assertNotNull($att2);
        $this->assertNotEmpty($att1->getMongoFileId());
        
        // Assert that they share the exact same MongoDB file ID
        $this->assertEquals($att1->getMongoFileId(), $att2->getMongoFileId());

        $mongoFileId = $att1->getMongoFileId();
        $storageService = $container->get(MongoFileStorageService::class);

        // Verify that the file exists in MongoDB GridFS
        $metadata = $storageService->getMetadata($mongoFileId);
        $this->assertNotNull($metadata);

        // 4. Delete the first attachment permanently (permanent = 1)
        $deleteUrl1 = sprintf('%s/%s?permanent=1', $url, $att1->getUuid());
        $client->request('DELETE', $deleteUrl1);
        $this->assertResponseStatusCodeSame(204);

        // Verify that the first entity is gone from SQL
        $em->clear();
        $att1Reloaded = $em->getRepository(TaskAttachment::class)->findOneBy(['uuid' => $data1['uuid']]);
        $this->assertNull($att1Reloaded);

        // But the MongoDB file MUST still exist because $att2 still references it!
        $metadataAfterFirstDelete = $storageService->getMetadata($mongoFileId);
        $this->assertNotNull($metadataAfterFirstDelete, 'GridFS file should not be deleted yet because it has another reference');

        // 5. Delete the second attachment permanently (permanent = 1)
        $deleteUrl2 = sprintf('%s/%s?permanent=1', $url, $att2->getUuid());
        $client->request('DELETE', $deleteUrl2);
        $this->assertResponseStatusCodeSame(204);

        // Verify that the second entity is gone from SQL
        $att2Reloaded = $em->getRepository(TaskAttachment::class)->findOneBy(['uuid' => $data2['uuid']]);
        $this->assertNull($att2Reloaded);

        // Now that the reference count is 0, the GridFS file MUST be deleted
        $metadataAfterSecondDelete = $storageService->getMetadata($mongoFileId);
        $this->assertNull($metadataAfterSecondDelete, 'GridFS file should be deleted after last reference is deleted');
    }
}
