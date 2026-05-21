<?php

namespace App\Security;

use Symfony\Component\Security\Core\Exception\AuthenticationException;

class ExpiredTokenException extends AuthenticationException
{
}
