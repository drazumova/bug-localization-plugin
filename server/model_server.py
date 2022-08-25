import grpc
import messages_pb2_grpc
import messages_pb2
import numpy as np
from concurrent import futures


class ModelServer(messages_pb2_grpc.ModelServicer):
    def getPrediction(self, request: messages_pb2.PredictionRequest,
                      context: grpc.aio.ServicerContext) \
            -> messages_pb2.PredictionResponse:
        times = np.array(list(map(lambda x: x.lastModificationTime, request.info.stacktrace)))
        max_index = np.argmax(times)
        result = np.zeros(times.shape)
        result[max_index] = 1.0
        return messages_pb2.PredictionResponse(probabilities=result)


def serve():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    messages_pb2_grpc.add_ModelServicer_to_server(ModelServer(), server)
    server.add_insecure_port('0.0.0.0:8080')
    server.start()
    server.wait_for_termination()


if __name__ == '__main__':
    serve()
