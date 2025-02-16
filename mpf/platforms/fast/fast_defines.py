"""Defines for FAST Pinball hardware."""

HARDWARE_KEY = {
    "neuron":  '2000'
}

USB_IDS = {  # (VID, PID)
    (11914, 4155): ('net', 'FAST Neuron Controller'),
    (11914, 4156): ('aud', 'FAST Audio Interface'),
    (11914, 4157): ('exp', 'FAST Expansion Board'),
    (11914, 4158): ('dsp', 'FAST Display Controller'),
    (5824, 1163):  ('net', 'FAST Retro Controller'),  # Teensyduino
    (1027, 24593): ('net', 'FAST Nano Controller'),  # FTDI Quad RS232-HS
}

VALID_IO_BOARDS = (
    'FP-I/O-3208',
    'FP-I/O-1616',
    'FP-I/O-1604',
    'FP-I/O-0804',
    'FP-I/O-0024',
    'FP-CAB-0001',   # Internally the same as FP-I/O-0024
    'FP-RETRO-I/O',  # Fake I/O board for Retro Controllers
)

EXPANSION_BOARD_FEATURES = {
    'FP-EXP-0061': {
        'min_fw': '0.31',
        'local_breakouts': ['FP-EXP-0061'],
        'breakout_ports': 0,
        'default_address': '90'
    },
    'FP-EXP-0071': {
        'min_fw': '0.11',
        'local_breakouts': ['FP-EXP-0071'],
        'breakout_ports': 0,
        'default_address': 'B4'
    },
    'FP-EXP-0081': {
        'min_fw': '0.12',
        'local_breakouts': ['FP-EXP-0081', 'FP-EXP-0081'],
        'breakout_ports': 0,
        'default_address': '84'
    },
    'FP-EXP-0091': {
        'min_fw': '0.11',
        'local_breakouts': ['FP-EXP-0091'],
        'breakout_ports': 2,
        'default_address': '88'
    },
    'FP-EXP-2000': {
        'min_fw': '0.11',
        'local_breakouts': ['FP-BRK-0001'],
        'breakout_ports': 3,
        'default_address': '48'
    },
    'FP-EXP-1313': {
        'min_fw': '0.01',
        'local_breakouts': ['FP-EXP-1313'],
        'breakout_ports': 0,
        'default_address': '30'
    },
}

BREAKOUT_FEATURES = {
    'FP-EXP-0061': {
        'min_fw': '0.33',
        'led_ports': 4,
        'stepper_ports': 2
    },
    'FP-EXP-0071': {
        'min_fw': '0.11',
        'led_ports': 4,
        'servo_ports': 4,
    },
    'FP-EXP-0081': {
        'min_fw': '0.11',
        'led_ports': 4,
    },
    'FP-EXP-0091': {
        'min_fw': '0.11',
        'led_ports': 4,
    },
    'FP-BRK-0001': {  # Neuron
        'min_fw': '0.8',
        'led_ports': 4,
    },
    'FP-DRV-0800': {
        'min_fw': '0.0',
        'servo_ports': 8,
    },
    'FP-BRK-0116': {
        'min_fw': '0.0',
        'flasher_ports': 16,
    },
    # TODO temp module until this code is written
    'FP-PWR-0007': {
        'min_fw': '0.0',
        'device_class': 'mpf.platforms.fast.fast_exp_board',
    },
    'FP-EXP-1313': {
        'min_fw': '0.1',
        'led_ports': 1,
    },
}