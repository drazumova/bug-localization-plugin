import cherrypy
import numpy as np


@cherrypy.expose
class Main(object):

    @cherrypy.tools.json_in()
    @cherrypy.tools.json_out()
    def POST(self):
        lines = cherrypy.request.json['lines']
        times = np.array(list(map(lambda x: x['lastModifiedTime'], lines)))
        max_index = np.argmax(times)
        result = np.zeros(len(lines))
        result[max_index] = 1.0
        return {"probabilities": result.tolist()}


if __name__ == '__main__':
    conf = {
        '/': {
            'request.dispatch': cherrypy.dispatch.MethodDispatcher(),
            'tools.sessions.on': True,
            'tools.response_headers.on': True,
            'tools.response_headers.headers': [('Content-Type', 'application/json')],
        }
    }
    cherrypy.config.update({'server.socket_host': '0.0.0.0',
                            'server.socket_port': 8080})
    cherrypy.quickstart(Main(), '/', conf)
