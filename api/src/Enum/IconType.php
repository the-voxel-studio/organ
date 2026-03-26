<?php

declare(strict_types=1);

namespace App\Enum;

enum IconType: string
{
    case SVG = 'SVG';
    case BLOB = 'BLOB';
    case EMOJI = 'EMOJI';
}
