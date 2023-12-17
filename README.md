# ConfessMe - Social Media App

ConfessMe is a social media platform developed to allow people to confess things to each other. Users can make confessions and respond to or like others' confessions. It is built using Firebase for cloud storage and incorporates various Android technologies. The codebase is structured using the MVVM architecture with a focus on clean code practices. The development is currently ongoing, and this Read Me will be regularly updated.

## App Screenshots

### Application Generral (Light and Dark Mode)

https://github.com/altaysoprano/ConfessMe/assets/37440249/af4f56b6-7598-4b1e-b05a-09a806fed1a1

https://github.com/altaysoprano/ConfessMe/assets/37440249/15349ae7-baaa-4693-9cb0-2b80bf319ab0

### Sign In - Sign Up Procedures
Here, there are 2 options available: signing in with a Google account and signing in with an email account. For more detailed information regarding what to consider in the Sign In, Sign Up, and Sign Out processes, you can examine the 'AuthRepositoryImp' class located in the data->repository file path of our project. Subsequently, to delve into what to consider in the SetProfile section, you may review the 'updateProfile()' function within the 'UserRepoImp' class in the data->repository file path. The 'updateProfile()' function is triggered when the 'Save' button is pressed in the SetProfileFragment. Each of these functions not only handles fundamental Firebase authentication tasks but also determines various operations and logics used within the application, such as token operations for notifications, updating all confessions and notifications during an update, and many similar processes.

https://github.com/altaysoprano/ConfessMe/assets/37440249/dbfd1247-12aa-4b58-825c-a36951c51426

### Edit Profile
On this screen, the user can set their nickname and bio. There's a character limit of 200 for the bio and 30 for the username. The username cannot be empty or less than 3 characters, and it cannot contain spaces. Here, the user can also remove or change their profile picture. After hitting save in either action, the user's previous photo in Firebase storage is deleted, preventing unnecessary bloating in the storage. Additionally, the user cannot claim a username that has been previously taken by another user. Each user's nickname must be unique to them.

https://github.com/altaysoprano/ConfessMe/assets/37440249/6a91acdf-0181-4714-952a-75c317da5dcc

### Confessing, Favorite, Answering

https://github.com/altaysoprano/ConfessMe/assets/37440249/8505f02f-2058-4832-8dc2-094d2e278d54

https://github.com/altaysoprano/ConfessMe/assets/37440249/4160042d-4a79-4052-aeb3-2f1119fc8b85

### Bookmarking

https://github.com/altaysoprano/ConfessMe/assets/37440249/15b962a2-d1fb-4d9a-8587-b1e19d568688

### Settings

https://github.com/altaysoprano/ConfessMe/assets/37440249/4f3ad134-4a1e-4b00-9c26-6184e5d80b3b

### Searching

https://github.com/altaysoprano/ConfessMe/assets/37440249/e46fe671-dbb4-48d0-b57a-ef71809d5786

### Following

https://github.com/altaysoprano/ConfessMe/assets/37440249/ecf76cb9-2847-471b-8095-e4a697448276

https://github.com/altaysoprano/ConfessMe/assets/37440249/d21f4dd3-ae2b-48e3-941a-18f76753569f

### Paging

https://github.com/altaysoprano/ConfessMe/assets/37440249/d2a98510-aa3a-44d7-aaf2-843c39f186b5
