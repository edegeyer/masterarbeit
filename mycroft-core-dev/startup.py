# file that will allow the service to be started from pycharm, so that debug points can be set
# just needs to start like debug would -> all services & cli
# see the .sh file for that

import mycroft.messagebus.service as bus
import mycroft.skills as skills
import mycroft.audio as audio
import mycroft.client.speech as voice
import  mycroft.client.text as cli
import mycroft.util.audio_test as audiotest
import mycroft.client.enclosure as enclosure


def main():
    #TODO: launch-all
    #TODO: launch-process cli
    launchbackground("hello")
    pass


def launchall():
    print("Starting all mycroft-core services")
    #TODO: launch-background bus
    # TODO: launch-background skills
    # TODO: launch-background audio
    # TODO: launch-background voice
    # TODO: launch-background enclosure


def launchbackground(module):
    skills()


if __name__ == '__main__':
    main()