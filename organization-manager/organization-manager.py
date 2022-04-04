#!/usr/bin/env python3

import os
import logging
import logging.config
import argparse
import requests
import xml.etree.ElementTree as ET
from requests.auth import HTTPBasicAuth
import json


LOG_FILE = f'/var/log/{os.path.basename(__file__).split(".")[0]}.log'
SRC_FILE = f'{os.path.abspath(os.path.dirname(__file__))}/config/organizations.xml'
JIRA_URL = ''
AUTH = HTTPBasicAuth('username', 'password')

logging.basicConfig(filename=LOG_FILE, format='%(asctime)s | %(name)s | %(levelname)s | %(message)s', datefmt='%Y-%m-%d %H:%M:%S', level=logging.DEBUG)


def add_groups_to_organization(organization_id, groups):
    tree = ET.parse(SRC_FILE)
    root = tree.getroot()
    selected_organization = root.find(f'.//*[@id="{organization_id}"]')
    if not selected_organization:
        logging.error(f'not found organization with id {organization_id}')
        return
    existing_groups = list(selected_organization.itertext())  # check if provided group exists in selected organization
    for group in groups:
        if group in existing_groups:
            logging.warning(f'group "{group}" did not added, already exists in "{selected_organization.attrib["name"]}"')
        else:
            ET.SubElement(selected_organization, 'group').text = group
            ET.indent(tree, space='\t', level=0)
            tree.write(SRC_FILE, xml_declaration=True, encoding='utf-8', short_empty_elements=False)
            logging.debug(f'group "{group}" added to "{selected_organization.attrib["name"]}"')


def update_organization_list():  # update file with organizations to have a complete list of organizations from Jira
    header = {
        "Accept": "application/json",
        "X-ExperimentalApi": "opt-in",
    }
    response = requests.request(
        'GET',
        JIRA_URL + '/rest/servicedeskapi/organization',
        headers=header,
        auth=AUTH
    )
    listed_organizations = {}  # organizations currently stored in file
    curr_organizations = {}  # actual organizations taken via API
    organizations_to_add = {}  # organizations which will be added to file if they are not, but exists on listed_organizations{}
    organizations_to_del = {}  # organization which will be deleted from file if they are not present in curr_organizations{}
    tree = ET.parse(SRC_FILE)
    root = tree.getroot()
    for organization in root.iter('organization'):  # list of organizations list taken from XML file (state from last update)
        listed_organizations[organization.attrib['id']] = organization.attrib['name']
    for organization in (json.loads(response.text))["values"]:  # actual list of organizations, taken from API
        curr_organizations[organization['id']] = organization['name']
    for org_id in curr_organizations:  # looking for organizations which need to be added to xml file
        if org_id not in listed_organizations:
            organizations_to_add[org_id] = curr_organizations[org_id]
    for org_id in listed_organizations:  # looking for organizations which was deleted in Jira, but still could be in file
        if org_id not in curr_organizations:
            organizations_to_del[org_id] = listed_organizations[org_id]
    if any(organizations_to_add):
        add_organizations_to_file(organizations_to_add)
    else:
        logging.info(f'no organizations to add in "{SRC_FILE}"')
    if any(organizations_to_del):
        del_organizations_from_file(organizations_to_del)
    else:
        logging.info(f'no organizations to delete from "{SRC_FILE}"')


def update_organization_users():  # check if organizations has all members of selected groups
    tree = ET.parse(SRC_FILE)
    organizations = tree.getroot()
    for organization in organizations:
        groups = []
        users_to_add = []
        users_to_del = []
        for group in organization.findall('group'):
            groups.append(group.text)
        if any(groups):
            listed_users = get_users_group(groups)
            curr_users = get_users_organization(organization.attrib['id'])
            for user in listed_users:
                if user not in curr_users:
                    users_to_add.append(user)
            for user in curr_users:
                if user not in listed_users:
                    users_to_del.append(user)
        if any(users_to_add):
            add_users_to_organization(users_to_add, organization.attrib['id'], organization.attrib['name'])
        if any(users_to_del):
            del_users_from_organization(users_to_del, organization.attrib['id'], organization.attrib['name'])


def add_users_to_organization(users, organization_id, organization_name):
    header = {
        'X-ExperimentalApi': 'opt-in',
        'Content-Type': 'application/json'
    }
    query = json.dumps({'usernames': users})
    requests.request(
        'POST',
        JIRA_URL + f'/rest/servicedeskapi/organization/{organization_id}/user',
        headers=header,
        data=query,
        auth=AUTH
    )
    logging.debug(f'{users} added to "{organization_id}: {organization_name}"')


def del_users_from_organization(users, organization_id, organization_name):
    header = {
        "X-ExperimentalApi": "opt-in",
        "Content-Type": "application/json"
    }
    query = json.dumps({"usernames": users})
    requests.request(
        'DELETE',
        JIRA_URL + f'/rest/servicedeskapi/organization/{organization_id}/user',
        headers=header,
        data=query,
        auth=AUTH
    )
    logging.debug(f'{users} deleted from "{organization_id}: {organization_name}"')


def add_organizations_to_file(organizations):
    for org_id in organizations:
        tree = ET.parse(SRC_FILE)
        root = tree.getroot()
        organization_to_add = ET.SubElement(ET.Element('organizations'), 'organization', name=organizations[org_id], id=org_id)
        root.append(organization_to_add)
        ET.indent(tree, space='\t', level=0)
        tree.write(SRC_FILE, xml_declaration=True, encoding='utf-8', short_empty_elements=False)
        logging.debug(f'"{organizations[org_id]}" added to "{SRC_FILE}"')


def del_organizations_from_file(organizations):
    for org_id in organizations:
        tree = ET.parse(SRC_FILE)
        root = tree.getroot()
        organization_to_del = root.find(f'.//*[@id="{org_id}"]')
        root.remove(organization_to_del)
        tree.write(SRC_FILE, xml_declaration=True, encoding='utf-8', short_empty_elements=False)
        logging.debug(f'"{organizations[org_id]}" deleted from "{SRC_FILE}"')


def get_users_organization(organization_id):
    user_list = []
    header = {
        "Accept": "application/json",
        "X-ExperimentalApi": "opt-in",
    }
    response = requests.request(
        'GET',
        JIRA_URL + f'/rest/servicedeskapi/organization/{organization_id}/user?limit=5000',
        headers=header,
        auth=AUTH
    )
    users = json.loads(response.text)['values']  # user list in organization
    for user in users:
        if user['active']:
            user_list.append(user['name'])
    return user_list


def get_users_group(groups):
    user_list = []
    for group in groups:
        header = {"Accept": "application/json"}
        query = {"groupname": group}
        response = requests.request(
            "GET",
            JIRA_URL + '/rest/api/latest/group/member',
            headers=header,
            params=query,
            auth=AUTH
        )
        try:
            users = json.loads(response.text)["values"]
            for user in users:
                if user['active'] and user['name'] not in user_list:
                    user_list.append(user['name'])
        except KeyError:
            logging.error(f'group "{group}" does not exists')
    return user_list


def list_organizations():
    tree = ET.parse(SRC_FILE)
    organizations = tree.getroot()
    organizations[:] = sorted(organizations, key=lambda child: (child.tag, int(child.get('id'))))
    tree.write(SRC_FILE, xml_declaration=True, encoding='utf-8', short_empty_elements=False)
    for organization in organizations:
        print(f'\t{organization.attrib["id"]}: {organization.attrib["name"]}')
        for group in organization:
            print(f'\t   - {group.text}')


def parse_args():
    arg_parser = argparse.ArgumentParser(description='Customer organization manager. Jira does not allow to add groups '
                                                     'to organizations (only single users), so thanks to this script it '
                                                     'is possible to manage members of specified organization stored in '
                                                     'one xml file, which location is defined as variable SRC_FILE.')
    arg_parser.add_argument('--update_list', help='update organizations list, if any no longer exists will be deleted from file, '
                                                  'also if any new was added to Jira - it will be added to file', action='store_true')
    arg_parser.add_argument('--update_org', help='update users in organization referring to groups in file', action='store_true')
    arg_parser.add_argument('--add_group', help='add group to organization, need to point organization as '
                                                '-o/--organization and group/s as -g/--group', action='store_true')
    arg_parser.add_argument('-o', '--organization', help="organization id, to check organization id's type -l/--list, "
                                                         "only usable with --add_group and -g/--group")
    arg_parser.add_argument('-g', '--group', nargs='+', help='group name, select which group add to pointed organization, '
                                                             'it is possible to provide multiple groups in format '
                                                             '[-g group1 group2 ... ], only usable with '
                                                             '--add_group and -o/--organization')
    arg_parser.add_argument('-l', '--list', help='lists organizations and groups stored in xml file, also sorts', action='store_true')
    return arg_parser.parse_args()


if __name__ == "__main__":
    args = parse_args()
    if args.add_group:
        if args.organization and args.group:
            add_groups_to_organization(args.organization, args.group)
        else:
            print(f'-g/--group and -o/--organization args expected if you want add users to organization from file')
    elif args.update_list:
        update_organization_list()
    elif args.update_org:
        update_organization_users()
    elif args.list:
        list_organizations()
