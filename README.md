# VisualVocab

**VisualVocab** is a gamified Android vocabulary-learning application that combines contextual language learning with human-guided object annotation.

Users can select photographs from their device, detect everyday objects, learn English–Spanish vocabulary from those objects, and complete interactive learning activities. Incorrect or missing object detections can also be corrected through **Creator Studio** and exported as reusable object-detection training data.

## Project Aim

The aim of VisualVocab was:

> To design, implement and evaluate a gamified mobile application that supports contextual vocabulary learning while enabling users to create reusable object-detection training data.

The project explores whether language-learning interaction can provide both:

- an immediate educational benefit for the user;
- a reusable technical output for future machine-learning workflows.

## Features

### Contextual Vocabulary Learning

- Select photographs from the Android device.
- Detect multiple objects within an image.
- Generate English and Spanish vocabulary from detected objects.
- English-to-Spanish multiple-choice questions.
- Spanish-to-English multiple-choice questions.
- Find-the-object activities using bounding boxes.
- English and Spanish text-to-speech pronunciation.
- Saved vocabulary and review sessions.

### Gamification

- Experience points (XP).
- Daily goals.
- Learning streaks.
- Achievements.
- Vocabulary mastery.
- Progress tracking.
- Immediate answer feedback.

### Object Detection

VisualVocab uses lightweight object-detection models designed for mobile inference.

The primary detector uses:

- **MediaPipe Tasks Vision**
- **EfficientDet**
- **TensorFlow Lite**

The project also includes an experimental **TensorFlow Lite YOLO detector**.

Where detections from multiple detectors overlap, confidence scores and **Intersection over Union (IoU)** are used to help suppress duplicate detections.

ML Kit image labelling is also used as supplementary contextual information.

## Creator Studio

Creator Studio provides the human-in-the-loop annotation workflow.

Users can:

- select an existing detection;
- correct its class label;
- manually draw a bounding box around a missed object;
- assign a new class label;
- save confirmed annotations locally;
- export the resulting dataset.

Automatic detections are therefore treated as a starting point rather than unquestioned ground truth.

## Dataset Export

Confirmed annotations can be exported as a ZIP archive containing:

```text
dataset/
├── images/
│   ├── image_1.jpg
│   └── image_2.jpg
│
├── labels/
│   ├── image_1.txt
│   └── image_2.txt
│
├── dataset.yaml
└── classes.json
