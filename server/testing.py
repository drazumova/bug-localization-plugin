import sys

import numpy as np
from sklearn import linear_model
import pandas as pd


class LinearModel:
    def __init__(self, n_features):
        self.weights = np.ones(n_features + 1)
        self.weights[-1] = 0

    def set_weights(self, new_weights):
        self.weights = new_weights

    def fit(self, X, y):
        pass

    def predict(self, X):
        return np.array(X) @ self.weights[:-1] + self.weights[-1]


def to_groups(rows):
    ids = set(rows['reportId'])

    groups = []
    for id in ids:
        stacktrace = rows[rows['reportId'] == id]
        if sum(stacktrace['label']) == 0:
            continue
        groups.append((id, stacktrace))

    return groups


def group_to_features(stacktrace):
    data = []
    labels = []
    for _, row in stacktrace.iterrows():
        stacktrace_features = [row['isFirstLine'], row['isLastModified']]
        data.append(stacktrace_features)
        labels.append(row['label'])
    return data, labels


def to_features(groups):
    data = []
    labels = []

    for (id, stacktrace) in groups:
        group_data, group_labels = group_to_features(stacktrace)
        data += group_data
        labels += group_labels
    return data, labels


def count_score(groups, model, k=1):
    cnt = 0
    for _, stacktrace in groups:
        y_true = stacktrace['label']
        fetures, labels = group_to_features(stacktrace)
        y_pred = model.predict(fetures)
        mx_indexes = np.array(y_pred).argsort()[-k:][::-1]
        if np.array(y_true)[mx_indexes].sum() > 0:
            cnt += 1
    print("score", cnt / len(groups))


if __name__ == '__main__':
    reports = sys.argv[1]
    all_lines = pd.read_csv(reports)
    all_lines = all_lines[all_lines['editable'] == 1]

    groups = to_groups(all_lines)
    print(len(groups))
    np.random.shuffle(groups)

    length = len(groups)
    coef = 0.8
    part = int(length * coef)

    groups_train = groups[:part]
    groups_test = groups[part:]

    X_train, y_train = to_features(groups_train)

    lin = linear_model.LinearRegression()
    lin.fit(X_train, y_train)
    print(lin.coef_)

    count_score(groups_test, lin)

    model = LinearModel(3)
    model.set_weights(np.array([1.0, 0.0, 0.0]))

    count_score(groups_test, model)

    model.set_weights(np.array([1.0, 1.0, 0.0]))

    count_score(groups_test, model)
