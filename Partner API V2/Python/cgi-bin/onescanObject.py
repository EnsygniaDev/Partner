import json

# Returns an empty object to construct onescan messages
# Optionally creates from a Json structure
class onescanObject(object):

    def __init__(self, j=None):
        if j != None:
            self.__dict__ = json.loads(j)
