import json

import numpy as np
from sklearn.metrics import classification_report


def parse_json_data(filename):
    reports = []
    with open(filename) as lines:
        for line in json.loads(lines.read()):
            reports.append(line)
    return reports


def labels(report):
    return list(map(lambda x: int(x["label"]), report["lines"]))


def blame_last_modified(report):
    modification_times = np.array(list(map(lambda x: int(x["lastModified"]), report["lines"])))
    predicted_labels = np.zeros_like(modification_times)
    max_element = np.max(modification_times)
    predicted_labels[modification_times == max_element] = 1
    return predicted_labels.tolist()


def blame_first_editable(report):
    modification_times = list(map(lambda x: int(x["lastModified"]), report["lines"]))
    predicted_labels = [0] * len(modification_times)
    for i in range(len(modification_times)):
        if modification_times[i] != -1:
            predicted_labels[i] = 1
            break
    return predicted_labels


def check(reports, model):
    all_labels = []
    all_predictions = []
    for report in reports:
        correct = labels(report)
        predicted = model(report)
        all_labels += correct
        all_predictions += predicted
        print("for report", report["reportId"])
        print(correct)
        print(predicted)
    print(classification_report(all_labels, all_predictions))


if __name__ == '__main__':
    reports = parse_json_data("../dataset_2.json")
    check(reports, blame_first_editable)


""" For last modified lines with file modification time as true label
        precision    recall  f1-score   support

           0       0.96      0.93      0.94      3761
           1       0.79      0.88      0.83      1186
"""

""" For first editable line with file modification as true label
        precision    recall  f1-score   support

           0       0.76      0.99      0.86      3761
           1       0.56      0.03      0.06      1186
"""
