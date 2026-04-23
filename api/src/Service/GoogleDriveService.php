<?php

declare(strict_types=1);

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;

class GoogleDriveService
{
    public function __construct(
        private readonly HttpClientInterface $httpClient,
        private readonly string $googleClientId,
        private readonly string $googleClientSecret
    ) {}

    /**
     * Exchange authorization code for tokens.
     */
    public function exchangeCode(string $code): ?array
    {
        try {
            $response = $this->httpClient->request('POST', 'https://oauth2.googleapis.com/token', [
                'body' => [
                    'client_id' => $this->googleClientId,
                    'client_secret' => $this->googleClientSecret,
                    'code' => $code,
                    'grant_type' => 'authorization_code',
                    'redirect_uri' => 'postmessage', // Important for popup flow
                ]
            ]);

            if ($response->getStatusCode() !== 200) {
                return null;
            }

            return $response->toArray();
        } catch (\Exception $e) {
            return null;
        }
    }

    /**
     * Get a fresh access token using a refresh token.
     */
    public function getAccessToken(string $refreshToken): ?string
    {
        try {
            $response = $this->httpClient->request('POST', 'https://oauth2.googleapis.com/token', [
                'body' => [
                    'client_id' => $this->googleClientId,
                    'client_secret' => $this->googleClientSecret,
                    'refresh_token' => $refreshToken,
                    'grant_type' => 'refresh_token',
                ]
            ]);

            if ($response->getStatusCode() !== 200) {
                return null;
            }

            $data = $response->toArray();
            return $data['access_token'] ?? null;
        } catch (\Exception $e) {
            return null;
        }
    }

    /**
     * Check if a file exists on Google Drive.
     */
    public function fileExists(string $accessToken, string $fileId): bool
    {
        try {
            $response = $this->httpClient->request('GET', 'https://www.googleapis.com/drive/v3/files/' . $fileId, [
                'headers' => [
                    'Authorization' => 'Bearer ' . $accessToken
                ],
                'query' => [
                    'fields' => 'id, trashed'
                ]
            ]);

            if ($response->getStatusCode() !== 200) return false;
            
            $data = $response->toArray();
            return !($data['trashed'] ?? false);
        } catch (\Exception $e) {
            return false;
        }
    }

    /**
     * Check existence of multiple files.
     * Returns array of existing file IDs.
     */
    public function checkFilesExistence(string $accessToken, array $fileIds): array
    {
        if (empty($fileIds)) return [];

        $existing = [];
        foreach ($fileIds as $id) {
            try {
                $response = $this->httpClient->request('GET', 'https://www.googleapis.com/drive/v3/files/' . $id, [
                    'headers' => [
                        'Authorization' => 'Bearer ' . $accessToken
                    ],
                    'query' => ['fields' => 'id, trashed']
                ]);

                if ($response->getStatusCode() === 200) {
                    $data = $response->toArray();
                    if (!($data['trashed'] ?? false)) {
                        $existing[] = $id;
                    }
                }
            } catch (\Exception $e) {
                // If error is not 404, assume it might still exist to be safe
                if ($e->getCode() !== 404) {
                    $existing[] = $id;
                }
            }
        }
        
        return $existing;
    }

    /**
     * List folders in the user's Drive.
     */
    public function listFolders(string $accessToken): array
    {
        try {
            $response = $this->httpClient->request('GET', 'https://www.googleapis.com/drive/v3/files', [
                'headers' => [
                    'Authorization' => 'Bearer ' . $accessToken
                ],
                'query' => [
                    'q' => "mimeType = 'application/vnd.google-apps.folder' and trashed = false",
                    'fields' => 'files(id, name)',
                    'pageSize' => 50
                ]
            ]);

            if ($response->getStatusCode() !== 200) {
                error_log("Google Drive List Error: " . $response->getContent(false));
                return [];
            }
            return $response->toArray()['files'] ?? [];
        } catch (\Exception $e) {
            error_log("Google Drive List Exception: " . $e->getMessage());
            return [];
        }
    }

    /**
     * Create a new folder in the user's Drive.
     */
    public function createFolder(string $accessToken, string $name, ?string $parentId = null): ?array
    {
        try {
            $jsonBody = [
                'name' => $name,
                'mimeType' => 'application/vnd.google-apps.folder',
                'description' => 'Organ Project Folder'
            ];

            if ($parentId) {
                $jsonBody['parents'] = [$parentId];
            }

            $response = $this->httpClient->request('POST', 'https://www.googleapis.com/drive/v3/files', [
                'headers' => [
                    'Authorization' => 'Bearer ' . $accessToken,
                    'Content-Type' => 'application/json'
                ],
                'json' => $jsonBody
            ]);

            if ($response->getStatusCode() !== 200) {
                error_log("Google Drive Create Error: " . $response->getContent(false));
                return null;
            }
            return $response->toArray();
        } catch (\Exception $e) {
            error_log("Google Drive Create Exception: " . $e->getMessage());
            return null;
        }
    }

    /**
     * Delete a file from Google Drive.
     */
    public function deleteFile(string $accessToken, string $fileId): bool
    {
        try {
            $response = $this->httpClient->request('DELETE', 'https://www.googleapis.com/drive/v3/files/' . $fileId, [
                'headers' => [
                    'Authorization' => 'Bearer ' . $accessToken
                ]
            ]);

            // 204 No Content is the success status for DELETE in Drive API
            return $response->getStatusCode() === 204;
        } catch (\Exception $e) {
            error_log("Google Drive Delete Error: " . $e->getMessage());
            return false;
        }
    }
}
