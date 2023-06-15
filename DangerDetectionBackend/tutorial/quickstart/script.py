# -*- coding: utf-8 -*-
"""
Created on Sat Mar 12 19:41:08 2022

@author: Ashikuzzaman Kanon
"""

from os.path import dirname, join
from tensorflow.keras.models import load_model
from kapre.time_frequency import STFT, Magnitude, ApplyFilterbank, MagnitudeToDecibel
import numpy as np
import pandas as pd
from librosa.core import resample, to_mono
import wavio
import tensorflow as tf
from itertools import chain


def envelope(y, rate, threshold):
    mask = []
    y = pd.Series(y).apply(np.abs)
    y_mean = y.rolling(window=int(rate/20),
                       min_periods=1,
                       center=True).max()
    for mean in y_mean:
        if mean > threshold:
            mask.append(True)
        else:
            mask.append(False)
    return mask, y_mean


def downsample_mono(obj, sr):
    
    deserialized_bytes = np.frombuffer(obj, dtype=np.float32)
    wav = np.resize(deserialized_bytes, (40000, 1))
    # wav = obj.data.astype(np.float32, order='F')
    # rate = obj.rate
    # print(wav.shape)
    try:
        channel = wav.shape[1]
        if channel == 2:
            wav = to_mono(wav.T)
        elif channel == 1:
            wav = to_mono(wav.reshape(-1))
    except IndexError:
        wav = to_mono(wav.reshape(-1))
        pass
    except Exception as exc:
        pass
    # wav = resample(wav, rate, sr)
    wav = wav.astype(np.int16)
    return sr, wav

sr = 40000
dt = 1.0
threshold = 20

def detectStatus(audio_obj):
    #return "Success"
    # file1 = join(dirname(__file__), "mobilenetv20.h5")
    model = load_model("tutorial/quickstart/model_and_audio/mobilenetv20.h5",
        custom_objects={'STFT':STFT,
                        'Magnitude':Magnitude,
                        'ApplyFilterbank':ApplyFilterbank,
                        'MagnitudeToDecibel':MagnitudeToDecibel})


    rate, wav = downsample_mono(audio_obj, sr)
    
    if len(wav.shape) == 2:
        wav=  wav.reshape((40000,))
    
    mask, env = envelope(wav, rate, threshold=threshold)
    clean_wav = wav[mask]
    step = int(sr*dt)
    batch = []

    for i in range(0, clean_wav.shape[0], step):
        sample = clean_wav[i:i+step]
        sample = sample.reshape(-1, 1)
        if sample.shape[0] < step:
            tmp = np.zeros(shape=(step, 1), dtype=np.float32)
            tmp[:sample.shape[0],:] = sample.flatten().reshape(-1, 1)
            sample = tmp
        batch.append(sample)
    X_batch = np.array(batch, dtype=np.float32)
    try:
        y_pred = model.predict(X_batch)
        y_mean = np.mean(y_pred, axis=0)
        y_pred = np.argmax(y_mean)
        return y_pred
    except:
        return -1
        
    
