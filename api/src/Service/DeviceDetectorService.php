<?php

declare(strict_types=1);

namespace App\Service;

use DeviceDetector\DeviceDetector;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Contracts\Cache\CacheInterface;
use Symfony\Contracts\Cache\ItemInterface;

class DeviceDetectorService
{
    public function __construct(
        private CacheInterface $cache
    ) {}

    public function getDeviceInfo(Request $request): array
    {
        $userAgent = $request->headers->get('User-Agent', '');
        
        $ip = $request->headers->get('CF-Connecting-IP') ?? $request->getClientIp();
        
        $dd = new DeviceDetector($userAgent);
        $dd->parse();

        $osInfo = $dd->getOs();
        $clientInfo = $dd->getClient();

        $location = 'Unknown Location';
        if ($ip && $ip !== '127.0.0.1' && $ip !== '::1') {
            $cacheKey = 'ip_location_' . str_replace([':', '.'], '_', $ip);
            
            $location = $this->cache->get($cacheKey, function (ItemInterface $item) use ($ip) {
                $item->expiresAfter(172800); // Mise en cache pendant 48 heures (172800 secondes)
                
                try {
                    $response = @file_get_contents("http://ip-api.com/json/{$ip}?fields=city,country");
                    if ($response) {
                        $data = json_decode($response, true);
                        if (isset($data['city'], $data['country'])) {
                            return "{$data['city']}, {$data['country']}";
                        }
                    }
                } catch (\Exception $e) {
                }
                
                return 'Unknown Location';
            });
        }

        return [
            'ipAddress' => $ip ?? '0.0.0.0',
            'userAgent' => $userAgent ?: 'None',
            'deviceName' => $osInfo['name'] ?? 'Generic Device',
            'browserName' => $clientInfo['name'] ?? 'Generic Browser',
            'location' => ($location && $location !== 'Unknown Location') ? $location : 'Anonymous Zone',
        ];
    }
}
