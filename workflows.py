#!/bin/python
# resolves : Is there a way to delete or hide old/renamed Workflows? #26256
# https://github.com/community/community/discussions/26256

from multiprocessing.dummy import Pool as ThreadPool
import requests

TOKEN = "{YOUR_GH_TOKEN}"
OWNER = "{OWNER}"
REPO = "{REPO}"
WORKFLOW = "{WORKFLOW_FILENAME}" # foo.yaml

headers = {
    "Accept": "application/vnd.github+json",
    "Authorization": f"Bearer {TOKEN}",
}

PER_PAGE = 100


def list_all_runs_for_workflow(owner, repo, workflow):
    url = f"https://api.github.com/repos/{owner}/{repo}/actions/workflows/{workflow}/runs"

    url_with_params = f"{url}?per_page=1"
    runs_response = requests.get(url, headers=headers).json()
    total_count = runs_response["total_count"]
    page_count = total_count // PER_PAGE + 1
    print("total_count: ", total_count)

    def _list_runs_by_page(page):
        url_with_params = f"{url}?page={page}&per_page={PER_PAGE}"
        runs_list = requests.get(url_with_params, headers=headers).json()
        page_workflow_runs = runs_list["workflow_runs"]

        return page_workflow_runs

    merged_workflow_runs = []
    with ThreadPool(10) as p:
        merged_workflow_runs = p.map(_list_runs_by_page, range(1, page_count + 1))

    return [run for runs in merged_workflow_runs for run in runs]

def remove_runs(owner, repo, runs_list):
    def remove_run_by_id(run_id):
        url = f"https://api.github.com/repos/{owner}/{repo}/actions/runs/{run_id}"
        requests.delete(url, headers=headers)

    for run in runs_list:
        remove_run_by_id(run["id"])

    print("removed runs: ", len(runs_list))


def main():
    runs = list_all_runs_for_workflow(OWNER, REPO, WORKFLOW)
    remove_runs(OWNER, REPO, runs)


if __name__ == "__main__":
    main()
