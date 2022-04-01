# !/usr/bin/env python3
# Author: K. Siedlaczek

import requests
import ftplib
import csv
import os
import re
import argparse
import smtplib
import xml.etree.ElementTree as ET
from datetime import datetime
from dateutil.relativedelta import relativedelta
from email.mime.multipart import MIMEMultipart
from email.mime.application import MIMEApplication

JIRA_URL = ''
FORMAT = 'xml'  # excel/xml
EXTERNAL_HOURS = 'false'  # default = true
ISSUE_DETAILS = 'true'
USER_DETAILS = 'true'
ISSUE_SUMMARY = 'true'
BILLING_INFO = 'false'
TEMPO_API_TOKEN = ''
SENDER_MAIL = ''
SENDER_PASSWORD = ''
SMTP_SERVER = ''
SMTP_PORT = 465


class TempoData:
    __slots__ = ['issue_key', 'issue_summary', 'hours', 'work_date', 
                 'full_name', 'period', 'month', 'issue_type', 
                 'issue_status', 'project_key', 'project_name']

    def __init__(self, issue_key, issue_summary, hours, work_date, full_name, period, month, issue_type, issue_status, project_key, project_name):
        self.issue_key = issue_key
        self.issue_summary = issue_summary
        self.hours = hours
        self.work_date = work_date
        self.full_name = full_name
        self.period = period
        self.month = month
        self.issue_type = issue_type
        self.issue_status = issue_status
        self.project_key = project_key
        self.project_name = project_name


def tempo_worklog(date_from, date_to, project_key, ftp_host, ftp_dir, ftp_user, ftp_pass, recipent_mails):
    worklog_filename = f'tempo-worklog_{project_key}_{date_from}_{date_to}.xml'
    url = f'{JIRA_URL}/plugins/servlet/tempo-getWorklog/' \
          f'?dateFrom={date_from}' \
          f'&dateTo={date_to}' \
          f'&format={FORMAT}' \
          f'&useExternalHours={EXTERNAL_HOURS}' \
          f'&addIssueDetails={ISSUE_DETAILS}' \
          f'&addUserDetails={USER_DETAILS}' \
          f'&addIssueSummary={ISSUE_SUMMARY}' \
          f'&addBillingInfo={BILLING_INFO}' \
          f'&tempoApiToken={TEMPO_API_TOKEN}' \
          f'&projectKey={project_key}'
    # print(url)
    response = requests.get(url)
    with open(worklog_filename, 'wb') as xml_file:
        xml_file.write(response.content)
    tempo_data_list = find_in_xml(worklog_filename)
    worklog_filename = worklog_filename.replace('.xml', '.csv')
    save_to_csv(worklog_filename, tempo_data_list)
    if recipent_mails:
        for mail in recipent_mails:
            send_to_mail(worklog_filename, mail, date_from, date_to)
        os.remove(worklog_filename)
    elif ftp_host:
        save_to_ftp(worklog_filename, ftp_host, ftp_dir, ftp_user, ftp_pass)
        os.remove(worklog_filename)


def find_in_xml(worklog_filename):  # find elements in tree and deletes .xml file
    tempo_data_list = []
    tree = ET.parse(worklog_filename)
    os.remove(worklog_filename)  # delete .xml file
    root = tree.getroot()
    for worklog in root:
        issue_key = worklog.find('issue_key').text
        issue_summary = worklog.find('issue_summary').text
        hours = worklog.find('hours').text
        work_date = worklog.find('work_date').text
        for user_details in worklog.iter('user_details'):
            full_name = user_details.find('full_name').text
        for issue_details in worklog.iter('issue_details'):
            issue_type = issue_details.find('type_name').text
            issue_status = issue_details.find('status_name').text
            project_key = issue_details.find('project_key').text
            project_name = issue_details.find('project_name').text
        period = datetime.strptime(work_date, '%Y-%m-%d').strftime('%m') + datetime.strptime(work_date, '%Y-%m-%d').strftime('%y')
        month = datetime.strptime(work_date, '%Y-%m-%d').strftime('%B')
        if re.search('.', hours):
            hours = hours.replace('.', ',')
        if re.search(';', issue_summary):
            issue_summary = issue_summary.replace(';', ',')
        if re.search('\t', issue_summary):
            issue_summary = issue_summary.replace('\t', ' ')
        tempo_data_list.append(TempoData(issue_key, issue_summary, hours, work_date, full_name, period, month, issue_type, issue_status, project_key, project_name))
    return tempo_data_list


def save_to_csv(worklog_filename, tempo_data_list):
    with open(worklog_filename, 'w', newline='', encoding='utf-16') as csv_file:
        writer = csv.writer(csv_file, delimiter=';')
        writer.writerow(['Issue Key', 'Issue Summary', 'Hours', 'Work date', 'Full Name', 'Period', 'Month', 'Issue Type', 'Issue Status', 'Project Key', 'Project Name'])
        for tempo_data in tempo_data_list:
            writer.writerow([tempo_data.issue_key, tempo_data.issue_summary, tempo_data.hours, tempo_data.work_date, tempo_data.full_name, tempo_data.period, tempo_data.month, tempo_data.issue_type, tempo_data.issue_status, tempo_data.project_key, tempo_data.project_name])


def save_to_ftp(input_file, ftp_host, ftp_dir, ftp_user, ftp_pass):
    directories = ftp_dir.split('/')
    ftp_session = ftplib.FTP(ftp_host)
    ftp_session.login(ftp_user, ftp_pass)
    ftp_session.cwd('/')
    for directory in directories:  # checks if ftp_dest_dir exists, if not mkdir
        if directory in ftp_session.nlst():
            ftp_session.cwd(directory)
        else:
            ftp_session.mkd(directory)
            ftp_session.cwd(directory)
    with open(input_file, 'rb') as output_file:
        ftp_session.storbinary(f'STOR /{ftp_dir}/{input_file}', output_file)
    ftp_session.quit()


def send_to_mail(input_file, recipent_mail, date_from, date_to):
    message = MIMEMultipart()
    message['Subject'] = f'Tempo worklog report from {date_from} to {date_to}'
    message['From'] = SENDER_MAIL
    message['To'] = recipent_mail
    with open(input_file, 'rb') as file:
        attachment = MIMEApplication(file.read(), filename=os.path.basename(input_file))
    attachment['Content-Disposition'] = 'attachment; filename=%s' % os.path.basename(input_file)
    message.attach(attachment)
    with smtplib.SMTP(SMTP_SERVER, SMTP_PORT) as server:
        server.starttls()
        server.login(SENDER_MAIL, SENDER_PASSWORD)
        server.sendmail(SENDER_MAIL, recipent_mail, message.as_string())


def _get_first_day_of_prev_month():
    return (datetime.now() + relativedelta(day=1, months=-1)).strftime('%Y-%m-%d')


def _get_first_day_of_curr_month():
    return (datetime.now() + relativedelta(day=1)).strftime('%Y-%m-%d')


def parse_args():
    arg_parser = argparse.ArgumentParser()
    arg_parser.add_argument('-b', '--beginDate', help='Date in format {-b yyyy-mm-dd}. It is not required, default value is first day of previous month', type=str, default=_get_first_day_of_prev_month())
    arg_parser.add_argument('-e', '--endDate', help='Date in format {-e yyyy-mm-dd}. It is not required, default value is last day of previous month', type=str, default=_get_first_day_of_curr_month())
    arg_parser.add_argument('-k', '--projectKey', help='Filter by project key in format {-pk projectKey}. It is not required, in this case script will generate billed hours for all project keys', type=str, default='')
    arg_parser.add_argument('-f', '--ftpHost', help='Dest FTP host in format {-fh host}. It is not required', type=str, default=False)
    arg_parser.add_argument('-d', '--ftpDir', help='Dest FTP dir in format {-fd path/to/dir}. It is not required', type=str)
    arg_parser.add_argument('-u', '--ftpUser', help='FTP username in format {-fu username}. It is not required', type=str)
    arg_parser.add_argument('-p', '--ftpPassword', help='FTP password in format {-fp password}. It is not required', type=str)
    arg_parser.add_argument('-m', '--recipientMail', nargs='+', help='Mail of recipient in format {-m email@} or {-m email1, email2}. It is not required', type=str, default=False)
    return arg_parser.parse_args()


if __name__ == '__main__':
    args = parse_args()
    tempo_worklog(str(args.beginDate), str(args.endDate), args.projectKey, args.ftpHost, args.ftpDir, args.ftpUser, args.ftpPassword, args.recipientMail)
