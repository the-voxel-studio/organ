<?php

namespace App\Controller;

use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

class GetTheAppController extends AbstractController
{
    #[Route('/get-the-app', name: 'app_get_the_app')]
    public function index(): Response
    {
        return $this->render('get_the_app/index.html.twig');
    }
}
