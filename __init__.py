# TODO: Add an appropriate license to your skill before publishing.  See
# the LICENSE file for more information.

# Below is the list of outside modules you'll be using in your skill.
# They might be built-in to Python, from mycroft-core or from external
# libraries.  If you use an external library, be sure to include it
# in the requirements.txt file so the library is installed properly
# when the skill gets installed later by a user.

from adapt.intent import IntentBuilder
from mycroft.skills.core import MycroftSkill, intent_handler
from mycroft.util.log import LOG
from mycroft.messagebus.message import Message

# Each skill is contained within its own class, which inherits base methods
# from the MycroftSkill class.  You extend this class as shown below.

# TODO: Change "Template" to a unique name for your skill
class TemplateSkill(MycroftSkill):

    # The constructor of the skill, which calls MycroftSkill's constructor
    def __init__(self):
        super(TemplateSkill, self).__init__(name="TemplateSkill")
        
        # Initialize working variables used within the skill.
        self.count = 0

    # The "handle_xxxx_intent" function is triggered by Mycroft when the
    # skill's intent is matched.  The intent is defined by the IntentBuilder()
    # pieces, and is triggered when the user's utterance matches the pattern
    # defined by the keywords.  In this case, the match occurs when one word
    # is found from each of the files:
    #    vocab/en-us/Hello.voc
    #    vocab/en-us/World.voc
    # In this example that means it would match on utterances like:
    #   'Hello world'
    #   'Howdy you great big world'
    #   'Greetings planet earth'


    @intent_handler(IntentBuilder("").require("person"))
    # TODO: see, if it's possible to call a skill in a better way
    def handle_person_detected_intent(self, message):
        self.set_context('personDetected', "true")
        self.speak_dialog("detected.person", expect_response=True)

    @intent_handler(IntentBuilder("").require("personDetected").require("confirmation"))
    def handle_direction_confirmation(self, message):
        answer = message.data["confirmation"]
        if answer == "yes":
            self.speak("Currently I'm only able to turn")
            self.speak("All commands have to be including the term turn and a direction", expect_response=True)
        elif answer == "no":
            self.speak("Ok, let me know when I can assist you")

    @intent_handler(IntentBuilder("").require("turn").require("Direction"))
    def handle_turn_intent(self,message):
        #direction = message.data.get("Direction")
        direction = message.data["Direction"]
        # hard coded seems better, as certain instructions need to be passed over to Loomo
        if direction == "right":
            pass
        elif direction == "left":
            pass
        elif direction == "around":
            pass
        else:
           # anything else the user says, that's in the Direction file, means that the roboter has to turn to the user
            pass
        self.bus.emit(Message(
            "loomoInstruction",
            {'action': 'turn',
             'direction': direction}))
        #self.speak_dialog("location.test", data={"direction": direction})


    @intent_handler(IntentBuilder("").require('action').require('direction').require('confirmation'))
    def handle_direction_confirmation_intent(self, message):
        # depending on the users answer, do the action or ask again what to do
        answer = message.data["confirmation"]
        action = message.data["action"]
        direction = message.data["direction"]
        if answer == "yes":
            self.speak("Will {} {}".format(action, direction))
            self.bus.emit(Message(
                "loomoInstruction",
                {'action': 'turn',
                 'direction': direction}))
        else:
            self.speak_dialog("misunderstood", expect_response=True)

    @intent_handler(IntentBuilder("").require('destAction').require('destination'))
    def handle_destination(self, message):
        action = message.data["destAction"]
        destination = message.data["destination"]
        self.set_context('actionHandle', action)
        self.set_context('destinationHandle', destination)
        if action == "get":
            self.speak_dialog("getting", expect_response=True)
            self.bus.emit(Message(
                "loomoInstruction",
                {'action': "getItem",
                 'item': destination}))
        else:
            self.speak("Going to {}".format(destination))
            self.bus.emit(Message(
                "loomoInstruction",
                {'action': "goPlace",
                 'destination': destination}))

    @intent_handler(IntentBuilder("").require('actionHandle').require('directionHandle').require('confirmation'))
    def handle_destination_confirmation(self, message):
        answer = message.data["confirmation"]
        action = message.data["actionHandle"]
        destination = message.data["directionHandle"]
        if answer == "yes":
            self.speak("OK")
            if action == "get":
                self.bus.emit(Message(
                    "loomoInstruction",
                    {'action': "getItem",
                     'item': destination}))
            else:
                self.bus.emit(Message(
                    "loomoInstruction",
                    {'action': "goPlace",
                     'destination': destination}))

    # The "stop" method defines what Mycroft does when told to stop during
    # the skill's execution. In this case, since the skill's functionality
    # is extremely simple, there is no need to override it.  If you DO
    # need to implement stop, you should return True to indicate you handled
    # it.
    #
    # def stop(self):
    #    return False

# The "create_skill()" method is used to create an instance of the skill.
# Note that it's outside the class itself.
def create_skill():
    return TemplateSkill()
