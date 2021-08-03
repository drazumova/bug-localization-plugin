## Python server

`main.py` runs CherryPy server.
It accepts json as messages format.

`model_server.py` is for GRPC communication model

## Dataset 

`testing.py` computes accuracy for sklearn linear regression model
and custom weights model. Features csv file path should be passed in the **first command line argument**.
The header should contain following columns.

``reportId, isFirstLine, isLastModified, editable, label``

`label` is used to check if this line needs to be fixed
