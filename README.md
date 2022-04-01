# jira-scripts
### `tempo-worklog.py`<br>
Creates a csv report with billed hours from Tempo<br>
Available arguments:
- `-b/--beginDate`	starting range of date in yyyy-mm-dd format, the default is the first day of the previous month
- `-e/--endDate`	ending date range in yyyy-mm-dd format, default is the first day of the current month
- `-k/--projectKey`	billed hours can be took from a specific project, just enter its key, in other case script will download billed hours from all projects
- `-f/--ftpHost`	host to which the report is to be sent
- `-d/--ftpDir`	path to which the report is to be sent (if it does not exist and the user has the appropriate permissions, it will be created)
- `-u/--ftpUser`	user that the script should use to send the report to FTP
- `-p/--ftpPassword`	Password needed to log in to FTP (if needed)
- `-m/--recipientMail`	Report recipient, you can enter several recipients in the format -m mail1 mail2 mail3

### `transitionRelatedIssueIfClosed.groovy`<br>
 Execute a workflow transition in order to change status of linked issue when issue triggered by event is closed. Linked issue needs to be from Service Desk project type. Need to add it as listener in Scriptrunner and assign the selected event from workflow
 
### `addDefaultWatchers.groovy`<br>
Adds a selected group of watchers to issue. Need to add it as listener in Scriptrunner and assign the 'Issue Created' event

### `organization-manager`<br>
Customer organization manager. Jira does not allow to add groups to organizations (only single users), so thanks to this script it is possible to manage members of specified organization stored in one xml file, which location is defined as variable `SRC_FILE`.
Available arguments:
- `--update_list`	command to update organizations list, if any no longer exists will be deleted from file, also if any new was added to Jira - it will be added to file
- `--update_org`	command to update users in organization referring to groups in file
- `--add_group`	command to add group to organization, need to point organization as `-o/--organization` and group/s as `-g/--group`
- `-o/--organization`	provide organization id value, to check organization id's type `-l/--list`, only usable with `--add_group` and `-g/--group`
- `-g/--group`	provide group name value, select which group add to pointed organization, it is possible to provide multiple groups in format `[-g group1 group2 ... ]`, only usable with `--add_group` and `-o/--organization`
- `-l/--list`	lists organizations and groups stored in xml file, also sorts
