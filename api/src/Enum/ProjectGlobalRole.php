<?php

declare(strict_types=1);

namespace App\Enum;

enum ProjectGlobalRole: string
{
    case ADMIN = 'ADMIN';
    case MEMBER = 'MEMBER';
}
