# TODO: Add an appropriate license to your skill before publishing.  See
# the LICENSE file for more information.

# Below is the list of outside modules you'll be using in your skill.
# They might be built-in to Python, from mycroft-core or from external
# libraries.  If you use an external library, be sure to include it
# in the requirements.txt file so the library is installed properly
# when the skill gets installed later by a user.

from adapt.intent import IntentBuilder
from mycroft.skills.core import MycroftSkill, intent_handler
from mycroft.messagebus.message import Message

# Each skill is contained within its own class, which inherits base methods
# from the MycroftSkill class.  You extend this class as shown below.

class LoomoSkill(MycroftSkill):

    # The constructor of the skill, which calls MycroftSkill's constructor
    def __init__(self):
        super(LoomoSkill, self).__init__(name="TemplateSkill")
        
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

    # TODO: Funktion muss noch richtig implementiert werden
    @intent_handler(IntentBuilder("").require("person"))
    def handle_person_detected_intent(self, message):
        self.set_context('personDetected', "true")
        self.speak_dialog("detected.person", expect_response=True)


    # TODO: person detected
    @intent_handler(IntentBuilder("").require("personDetected").require("confirmation"))
    def handle_direction_confirmation(self, message):
        answer = message.data["confirmation.voc"]
        if answer == "yes":
            self.speak("Currently I'm only able to turn")
            self.speak("All commands have to be including the term turn and a direction", expect_response=True)
        elif answer == "no":
            self.speak("Ok, let me know when I can assist you")

    @intent_handler(IntentBuilder("").require("comeback"))
    def handle_comeBack(self, message):
        self.bus.emit(Message(
            "loomoInstruction",
            {'action' : "comeback"}
        ))

    @intent_handler(IntentBuilder("").require("straight"))
    def handle_goahead(self, message):
        self.bus.emit(Message(
            "loomoInstruction",
            {"action" : "straight"}
        ))


    @intent_handler(IntentBuilder("").require("turn").require("Direction"))
    def handle_turn_intent(self,message):
        direction = message.data["Direction"]
        # Message Bus gets the information, that turning is needed & the direction
        self.bus.emit(Message(
            "loomoInstruction",
            {'action': 'turn',
             'direction': direction}))

    @intent_handler(IntentBuilder("").require("go").require("destination"))
    def handle_destination(self, message):
        destination = message.data['destination']
        self.speak("Going to " + destination)
        self.bus.emit(Message(
            "loomoInstruction",
            {'action': "goPlace",
             'destination':destination}
        ))

    @intent_handler(IntentBuilder("").require("get").require("item"))
    def handle_get_item(self, message):
        item = message.data["item"]
        self.speak("Getting " + item)
        self.bus.emit(Message(
            "loomoInstruction",
            {'action': "getItem",
             'item': item}
        ))


    @intent_handler(IntentBuilder("").require('outofmyway'))
    def go_out_of_way(self, message):
        self.bus.emit(Message(
            "loomoInstruction",
            {"action": "outofway"}
        ))



    # TODO: Skill to always stop the current action
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
    return LoomoSkill()
