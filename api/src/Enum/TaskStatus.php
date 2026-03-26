<?php

declare(strict_types=1);

namespace App\Enum;

enum TaskStatus: string
{
    case TODO = 'TODO';
    case IN_PROGRESS = 'IN_PROGRESS';
    case WAITING = 'WAITING';
    case DONE = 'DONE';
    case CANCELED = 'CANCELED';
}
