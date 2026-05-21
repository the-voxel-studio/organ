<?php

namespace App\Controller;

trait ProjectIdentityTrait
{
    private function extractIdentity(array $project): array
    {
        $default = ['color' => '#FF7EB6', 'iconName' => 'icon_1'];
        
        $color = $project['color'] ?? null;
        $iconName = null;

        if (isset($project['iconType']) && $project['iconType'] === 'SVG' && isset($project['iconData']) && str_starts_with($project['iconData'], '{')) {
            $data = json_decode($project['iconData'], true);
            if (!$color) {
                $color = $data['color'] ?? null;
            }
            $iconName = $data['icon'] ?? null;
        }

        return [
            'color' => $color ?? $default['color'],
            'iconName' => $iconName ?? $default['iconName']
        ];
    }
}
