#  Copyright 2023 Karl Kegel
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
from typing import Optional

from botdriver.messages.message_target import MessageTarget
from botdriver.messages.message_type import MessageType


class Message:
    message_type: MessageType = None
    message_target: Optional[MessageTarget] = None

    def __init__(self, message_type: MessageType, message_target: Optional[MessageTarget] = None):
        self.message_type = message_type
        self.message_target = message_target

    def assign_target(self, message_target) -> None:
        self.message_target = message_target
