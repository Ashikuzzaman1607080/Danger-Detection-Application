from django.shortcuts import render

# Create your views here.
from django.contrib.auth.models import User, Group
from rest_framework import viewsets
from rest_framework import permissions
from tutorial.quickstart.serializers import StatusSerializer, GroupSerializer
import base64
import wavio

from rest_framework.response import Response

# from .script import 0
from rest_framework.decorators import action
from .script import detectStatus


class StatusViewSet(viewsets.ViewSet):
    """
    API endpoint that allows users to be viewed or edited.
    """
    
    @action(detail=True, methods=['post'])
    def get_status(self, request, pk, format=None):
        # obj = wavio.read("tutorial/quickstart/model_and_audio/testRecordingFle.wav")
        try:
            encode_string=request.data.get('encodedMP3')
            decode_string = base64.b64decode(encode_string)
            status = detectStatus(decode_string)
            print('ok', status)
            return Response(data=status)
        except:
            print("exvept")
            return Response(data=-1)

    @action(detail=True, methods=['post'])
    def test(self, request, pk, format=None):
        encode_string=request.data.get('encodedMP3')
        
        wav_file = open("temp.wav", "wb")
        decode_string = base64.b64decode(encode_string)
        # print('decode_string', encode_string)
        wav_file.write(decode_string)
        # raise Exception
        
        return Response(data='Fine')


class GroupViewSet(viewsets.ModelViewSet):
    """
    API endpoint that allows groups to be viewed or edited.
    """
    queryset = Group.objects.all()
    serializer_class = GroupSerializer