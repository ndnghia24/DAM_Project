// prisma/seed.js
const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

async function main() {
    // Tạo người dùng
    const user1 = await prisma.users.create({
        data: {
            username: 'user1',
            password: 'password123',
        },
    });

    const user2 = await prisma.users.create({
        data: {
            username: 'user2',
            password: 'password456',
        },
    });

    const user3 = await prisma.users.create({
        data: {
            username: 'user3',
            password: 'password789',
        },
    });

    // Tạo bài viết
    const post1 = await prisma.posts.create({
        data: {
            content: 'Post by user1',
            userId: user1.username,
        },
    });

    const post2 = await prisma.posts.create({
        data: {
            content: 'Another post by user1',
            userId: user1.username,
        },
    });

    const post3 = await prisma.posts.create({
        data: {
            content: 'Post by user2',
            userId: user2.username,
        },
    });

    // Tạo bình luận
    await prisma.comments.create({
        data: {
            content: 'Great post!',
            userId: user2.username,
            postId: post1.id,
        },
    });

    await prisma.comments.create({
        data: {
            content: 'Interesting thoughts.',
            userId: user3.username,
            postId: post1.id,
        },
    });

    await prisma.comments.create({
        data: {
            content: 'Nice work!',
            userId: user1.username,
            postId: post3.id,
        },
    });

    console.log('Seed data created successfully.');
}

main()
    .catch((e) => {
        console.error(e);
        process.exit(1);
    })
    .finally(async () => {
        await prisma.$disconnect();
    });
