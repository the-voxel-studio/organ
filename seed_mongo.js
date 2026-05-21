// MongoDB Seed Script for Organ Project
// Connect to organ_dev database
const dbName = 'organ_dev';
const db = db.getSiblingDB(dbName);

// Clear existing data
db.audit_logs.deleteMany({});
db.daily_stats.deleteMany({});
db.fs.files.deleteMany({});
db.fs.chunks.deleteMany({});

// Fixed UUIDs (matching seed.sql)
const userUuid = '550e8400-e29b-41d4-a716-446655440000'; // Admin
const projectUuid = '660e8400-e29b-41d4-a716-446655440000';
const organUuid = '770e8400-e29b-41d4-a716-446655440000';
const task1Uuid = '880e8400-e29b-41d4-a716-446655440000';
const task2Uuid = '880e8400-e29b-41d4-a716-446655440001';

const now = new Date();
const yesterday = new Date(now);
yesterday.setDate(now.getDate() - 1);

// 1. Seed Audit Logs
db.audit_logs.insertMany([
    {
        taskUuid: task1Uuid,
        organUuid: organUuid,
        projectUuid: projectUuid,
        userUuid: userUuid,
        actionType: 'CREATE',
        fieldName: null,
        oldValues: null,
        newValues: { title: 'Design API Auth', status: 'TODO' },
        context: { ip: '127.0.0.1', method: 'POST' },
        createdAt: yesterday
    },
    {
        taskUuid: task1Uuid,
        organUuid: organUuid,
        projectUuid: projectUuid,
        userUuid: userUuid,
        actionType: 'STATUS_CHANGE',
        fieldName: 'status',
        oldValues: { status: 'TODO' },
        newValues: { status: 'DONE' },
        context: { ip: '127.0.0.1', method: 'PATCH' },
        createdAt: now
    },
    {
        taskUuid: task2Uuid,
        organUuid: organUuid,
        projectUuid: projectUuid,
        userUuid: userUuid,
        actionType: 'CREATE',
        fieldName: null,
        oldValues: null,
        newValues: { title: 'Organ CRUD', status: 'TODO' },
        context: { ip: '127.0.0.1', method: 'POST' },
        createdAt: yesterday
    }
]);

// 2. Seed Daily Stats
db.daily_stats.insertMany([
    {
        date: new Date(yesterday.setHours(0,0,0,0)),
        projectUuid: projectUuid,
        organUuid: organUuid,
        tasksCreated: 2,
        tasksCompleted: 0,
        tasksCanceled: 0,
        commentsAdded: 0,
        attachmentsAdded: 0,
        attachmentsSize: 0,
        membersActive: 1,
        statusChanges: { 'NONE_TO_TODO': 2 },
        extra: { active_users: [userUuid] }
    },
    {
        date: new Date(now.setHours(0,0,0,0)),
        projectUuid: projectUuid,
        organUuid: organUuid,
        tasksCreated: 0,
        tasksCompleted: 1,
        tasksCanceled: 0,
        commentsAdded: 1,
        attachmentsAdded: 1,
        attachmentsSize: 1048576,
        membersActive: 1,
        statusChanges: { 'TODO_TO_DONE': 1 },
        extra: { active_users: [userUuid] }
    }
]);

// 3. Seed GridFS Mock File (Metadata only for simplicity in seed)
const fileId = ObjectId("645e12345678901234567890");
db.fs.files.insertOne({
    _id: fileId,
    length: 1048576,
    chunkSize: 261120,
    uploadDate: now,
    filename: "architecture.png",
    metadata: {
        taskAttachmentUuid: "some-random-uuid-for-file",
        mimeType: "image/png",
        size: 1048576,
        checksum: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
        version: 1
    }
});

print('MongoDB seed completed successfully!');
