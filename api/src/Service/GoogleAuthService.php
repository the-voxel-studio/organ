<?php

declare(strict_types=1);

namespace App\Service;

use Symfony\Component\HttpFoundation\Response;
use Symfony\Contracts\HttpClient\HttpClientInterface;

class GoogleAuthService
{
    public function __construct(
        private readonly string $googleClientId
    ) {
    }

    public function verifyToken(string $idToken): ?array
    {
        $url = "https://oauth2.googleapis.com/tokeninfo?id_token=" . $idToken;
        
        try {
            $response = file_get_contents($url);
            if ($response === false) {
                return null;
            }

            $data = json_decode($response, true);

            if (!isset($data['email']) || !isset($data['aud'])) {
                return null;
            }

            // Verify audience matches our Client ID
            if ($data['aud'] !== $this->googleClientId) {
                return null;
            }

            return [
                'email' => $data['email'],
                'googleId' => $data['sub'],
                'firstName' => $data['given_name'] ?? null,
                'lastName' => $data['family_name'] ?? null,
                'emailVerified' => $data['email_verified'] ?? false,
            ];
        } catch (\Exception $e) {
            return null;
        }
    }
}
