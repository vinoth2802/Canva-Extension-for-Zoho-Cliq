# Canva-Extension-for-Zoho-Cliq
Canva Extension for Zoho cliq that enables the user to view their canva projects in their cliq
## Features
### /viewprojects
This slash command lets users view their canva projects in the Zoho cliq platform. This command fetches the Canva projects and displays them with their project name, view URL, edit URL in a tabular format.

### OAuth 2.0 authentication with PCKE specification
Implemented OAuth 2.0 authentication with PKCE (Proof Key for Code Exchange) to enhance the security of authorization code flow in public or client-side applications. This feature prevents authorization code interception attacks by introducing a dynamic code challenge and verifier.

## Usage Guide

### Introduction
This extension integrates Canva with Zoho Cliq, allowing users to seamlessly manage and access their Canva projects within Zoho Cliq. It provides the following key functionality:

**Authorize Your Account:** Authenticate your Canva account securely. <br/>
**View Projects:** Use the /viewprojects slash command to list all your Canva projects along with their view and edit URLs.

### Steps to Use
### 1. Install the Extension
Ensure the extension is installed and activated in your Zoho Cliq workspace.

### 2. Authorize Canva Account
Use the slash command /viewprojects , this will display the authorization message if you are an initial user.

![ALT TEXT](https://github.com/vinoth2802/Canva-Extension-for-Zoho-Cliq/blob/main/Screenshots/Screenshot%202024-12-29%20121102.png)

Once you click the authorize button, you will be redirected to the canva's authorization page.

![ALT TEXT](https://github.com/vinoth2802/Canva-Extension-for-Zoho-Cliq/blob/main/Screenshots/Screenshot%202024-12-29%20121114.png)

Click allow access and you will be redirected to the Authorization Success page.

![ALT TEXT](https://github.com/vinoth2802/Canva-Extension-for-Zoho-Cliq/blob/main/Screenshots/Screenshot%202024-12-29%20121129.png)

Once authenticated, the extension will have access to your Canva project data. Now close the success page and return to your Zoho cliq.

### 3. List Canva Projects
Enter the command /viewprojects in any Zoho Cliq chat.
The extension will display a list of your Canva projects, each accompanied by:

**Project Name:**  Name of the project <br/>
**View URL:** To preview the project. <br/>
**Edit URL:** To directly edit the project. <br/>

![ALT TEXT](https://github.com/vinoth2802/Canva-Extension-for-Zoho-Cliq/blob/main/Screenshots/Screenshot%202024-12-29%20121215.png)

This extension also uses the tinyURL platform to shorten the view and edit URLs.
