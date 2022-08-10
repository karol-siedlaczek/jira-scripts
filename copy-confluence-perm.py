import argparse
import json
import requests
from requests.auth import HTTPBasicAuth
from getpass import getpass

CONFLUENCE_URL = 'confluence_url'


def get_spaces_list(group, auth):
    response = requests.request(
        'GET',
        CONFLUENCE_URL + f'/rest/extender/1.0/permission/group/{group}/getAllSpacesWithPermissions?spacesAsArray=true',
        auth=auth
    )
    if not (response.ok):
        raise Exception(f'{response.status_code} {response.text}')
    else:
        return json.loads(response.text)


def duplicate_permissions(group, result, auth):
    for space in result['spaces']:
        response = requests.request(
            'PUT',
            CONFLUENCE_URL + f"/rest/extender/1.0/permission/space/{space['key']}/group/{group}/addSpacePermissions",
            data=json.dumps({'permissions': space['permissions']}),
            auth=auth
        )
        if not (response.ok):
            print(f'{response.status_code} {response.text}')
        else:
            print(f"permissions '{space['permissions']}' added for '{group}' to '{space['key']}'")


def parse_args():
    arg_parser = argparse.ArgumentParser(description='')
    arg_parser.add_argument('-s', '--srcGroup', help='', type=str, required=True)
    arg_parser.add_argument('-d', '--destGroup', help='', type=str, required=True)
    arg_parser.add_argument('-u', '--username', help='', type=str, required=True)
    return arg_parser.parse_args()


if __name__ == "__main__":
    args = parse_args()
    password = getpass(prompt=f"Enter password for user '{args.username}': ")
    auth = HTTPBasicAuth(args.username, password)
    response = get_spaces_list(args.srcGroup, auth)
    duplicate_permissions(args.destGroup, response, auth)
