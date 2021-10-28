# Pixivj Integration Test
This repository contains the integration test for Pixivj

# Run the test
Run
```bash
mvn test
```
You have to run it within an environment that supports UI because the test will open a window using
JavaFX, and you have to manually login with your Pixiv account. The test will use your account to
run the test. Though the test won't do dangerous operations using your Pixiv account, it will
change your account state. For example, it will use your account to add a bookmark. If you have 
concern, use an account that you don't regularly use.  
If you don't want to login for every run, you can add an environment variable to show to access
token.
```bash
SHOW_ACCESS_TOKEN=true mvn test
```
Then, you can copy the access token, and run again with
```bash
ACCESS_TOKEN={your_access_token} mvn test
```
This time, you don't have to login.  
Notice that access token will expire after a while, and you have to obtain a new one again.
 