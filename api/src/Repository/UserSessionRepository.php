<?php

declare(strict_types=1);

namespace App\Repository;

use App\Entity\UserSession;
use App\Entity\User;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;
use Gesdinet\JWTRefreshTokenBundle\Doctrine\RefreshTokenRepositoryInterface;

/**
 * @extends ServiceEntityRepository<UserSession>
 */
class UserSessionRepository extends ServiceEntityRepository implements RefreshTokenRepositoryInterface
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, UserSession::class);
    }

    /**
     * @param \DateTimeInterface|null $datetime
     * @return UserSession[]
     */
    public function findInvalid(\DateTimeInterface $datetime = null): array
    {
        $datetime = $datetime ?: new \DateTime();

        return $this->createQueryBuilder('u')
            ->where('u.valid < :datetime')
            ->setParameter('datetime', $datetime)
            ->getQuery()
            ->getResult();
    }

    /**
     * @param \DateTimeInterface|null $datetime
     * @param int|null $batchSize
     * @param int $offset
     * @return \Traversable|array
     */
    public function findInvalidBatch(\DateTimeInterface $datetime = null, ?int $batchSize = null, int $offset = 0): \Traversable|array
    {
        $datetime = $datetime ?: new \DateTime();

        $queryBuilder = $this->createQueryBuilder('u')
            ->where('u.valid < :datetime')
            ->setParameter('datetime', $datetime)
            ->setFirstResult($offset);

        if (null !== $batchSize) {
            $queryBuilder->setMaxResults($batchSize);
        }

        return $queryBuilder->getQuery()->getResult();
    }

    public function findExistingSession(User $user, string $location, string $deviceName, string $browserName): ?UserSession
    {
        return $this->createQueryBuilder('s')
            ->where('s.user = :user')
            ->andWhere('s.location = :location')
            ->andWhere('s.deviceName = :deviceName')
            ->andWhere('s.browserName = :browserName')
            ->andWhere('s.valid > :now')
            ->setParameter('user', $user)
            ->setParameter('location', $location)
            ->setParameter('deviceName', $deviceName)
            ->setParameter('browserName', $browserName)
            ->setParameter('now', new \DateTime())
            ->getQuery()
            ->getOneOrNullResult();
    }
}
