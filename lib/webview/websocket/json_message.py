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
import json

from lib.webview.websocket.message import Message
from lib.webview.websocket.message_type import MessageType


class JsonMessage(Message):

    __data: dict = None

    def __init__(self):
        super().__init__(MessageType.JSON)

    def set_data(self, data: dict) -> None:
        self.__data = data

    def get_data(self) -> dict:
        return self.__data

    def serialize(self) -> str:
        return json.dumps(self.__data)
