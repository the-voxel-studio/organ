<?php

namespace App\Command;

use App\Service\StatsAggregationService;
use Symfony\Component\Console\Attribute\AsCommand;
use Symfony\Component\Console\Command\Command;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Input\InputOption;
use Symfony\Component\Console\Output\OutputInterface;
use Symfony\Component\Console\Style\SymfonyStyle;

#[AsCommand(
    name: 'app:stats:aggregate',
    description: 'Aggregates daily statistics from audit logs into MongoDB.',
)]
class StatsAggregateCommand extends Command
{
    public function __construct(
        private StatsAggregationService $statsAggregationService
    ) {
        parent::__construct();
    }

    protected function configure(): void
    {
        $this
            ->addOption('date', 'd', InputOption::VALUE_REQUIRED, 'Date to aggregate (YYYY-MM-DD)', date('Y-m-d'))
        ;
    }

    protected function execute(InputInterface $input, OutputInterface $output): int
    {
        $io = new SymfonyStyle($input, $output);
        $dateStr = $input->getOption('date');

        try {
            $date = new \DateTime($dateStr);
        } catch (\Exception $e) {
            $io->error('Invalid date format. Use YYYY-MM-DD.');
            return Command::FAILURE;
        }

        $io->title(sprintf('Aggregating stats for %s', $date->format('Y-m-d')));

        try {
            $this->statsAggregationService->aggregateForDate($date);
            $io->success('Statistics aggregated successfully.');
            return Command::SUCCESS;
        } catch (\Exception $e) {
            $io->error(sprintf('An error occurred: %s', $e->getMessage()));
            return Command::FAILURE;
        }
    }
}
