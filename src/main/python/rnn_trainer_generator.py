import random
import math
import numpy as np
import tensorflow as tf
import cmath
from sys import argv
from sys import exit
from functools import reduce

def main():
    try:
        mode = argv[1]
        if (mode != 'train' and mode != 'generate'):
            raise ValueError
    except (IndexError, ValueError) as e:
        print('Usage: python {} [train|generate] [...]'.format(argv[0]))
        return

    if mode == 'train':
        filenames = argv[2:]
        if filenames == []:
            print('Usage: python {} train [filenames]'.format(argv[0]))
            return
        dataset = make_dataset(filenames)

        model = Model()
        model.train(dataset, None, 1000, 0.2)

    elif mode == 'generate':
        try:
            checkpoint_file = argv[2]
            seed_file = argv[3]
            num_notes = int(argv[4])
        except (IndexError, ValueError) as e:
            print('Usage: python {} generate [checkpoint file] [seed file] [num notes]'.format(argv[0]))
            return

        with open(seed_file) as f:
            seed = normalize_line(f.readline())

        model = Model()
        generated = model.generate(checkpoint_file, seed, num_notes)

        for note in generated:
            print(denormalize_line_deterministic(note))

class Model:
    def __init__(self):
        self.look_back = 50
        self.minibatch_size = 8
        self.features = 53
        self.num_hidden_units = 128
        # self.learning_rate = 0.001
        self.pad_note = [2.0] * self.features

        # weights and biases of final dense layers (shared)
        out_weights = tf.Variable(tf.random_normal([self.num_hidden_units, self.features]))
        out_bias = tf.Variable(tf.random_normal([self.features]))

        # placeholder for subsequence of the piece
        self.x = tf.placeholder('float32', [None, self.look_back, self.features])
        # placeholder for target output (next note predictions for each input note)
        self.y = tf.placeholder('float32', [None, self.look_back, self.features])
        # placeholder for initial state of lstm
        self.initial_c_state = tf.placeholder('float32', [None, self.num_hidden_units])
        self.initial_h_state = tf.placeholder('float32', [None, self.num_hidden_units])
        # decaying learning rate
        self.learning_rate = tf.placeholder('float32', [])

        # processing the input tensor from [batch_size,n_steps,n_input] to "time_steps" number of [batch_size,n_input] tensors
        inputs = tf.unstack(self.x, self.look_back, 1)
        # same for expected output
        expected_outputs = tf.unstack(self.y, self.look_back, 1)

        lstm = tf.contrib.rnn.BasicLSTMCell(self.num_hidden_units)
        state_pair = (self.initial_c_state, self.initial_h_state)
        outputs, _ = tf.contrib.rnn.static_rnn(lstm, inputs, initial_state=state_pair)
        # need to know new state after just one timestep so we can move the window
        next_output, outputted_state_pair = lstm(inputs[0], state_pair)
        self.next_c_state = outputted_state_pair.c
        self.next_h_state = outputted_state_pair.h
        self.next_prediction = compound_activation(tf.matmul(next_output, out_weights) + out_bias)
        # make next note predictions for every single input
        predictions = [compound_activation(tf.matmul(outputs[i], out_weights) + out_bias) for i in range(self.look_back)]
        self.loss = sum([compound_loss(expected_outputs[i], predictions[i]) for i in range(self.look_back)])
        self.loss = self.loss / self.look_back
        self.opt = tf.train.AdamOptimizer(learning_rate=self.learning_rate).minimize(self.loss)

    def train(self, training_set, validation_set, num_iterations, note_limit_increase):
        # note_limit_increase is by how much we increase the note limit after each iteration

        num_saves = 50
        saver = tf.train.Saver()
        init = tf.global_variables_initializer()

        # limit how far into the piece we train, then gradually increase limit
        note_limit = float(self.look_back + 1)

        with tf.Session() as sess:
            sess.run(init)
            iteration = 0
            initial_learning_rate = 0.001
            for iteration in range(num_iterations):
                learn_rate = initial_learning_rate * (float(self.look_back) / note_limit)
                print('Iteration {} ({} notes) (learning rate is {})'.format(iteration, int(note_limit), learn_rate))
                training_loss = 0.0
                # pick minibatch of pieces
                minibatch = self.make_randomized_padded_minibatch(training_set, self.minibatch_size)
                # initialize lstm state
                c_state = np.zeros([self.minibatch_size, self.num_hidden_units])
                h_state = np.zeros([self.minibatch_size, self.num_hidden_units])
                # move window over piece
                for pos in range(len(minibatch[0]) - self.look_back - 1):
                    if pos + self.look_back >= int(note_limit):
                        break

                    data_in = minibatch[:, pos : pos+self.look_back]
                    assert data_in.shape == (self.minibatch_size, self.look_back, self.features)
                    labels = minibatch[:, pos+1 : pos+self.look_back+1]
                    assert labels.shape == (self.minibatch_size, self.look_back, self.features)

                    _ , c_state, h_state = sess.run(
                        [self.opt, self.next_c_state, self.next_h_state],
                        feed_dict={self.x: data_in, self.y: labels, self.learning_rate: learn_rate,
                        self.initial_c_state: c_state, self.initial_h_state: h_state})

                    training_loss += sess.run(self.loss,
                        feed_dict={self.x: data_in, self.y: labels, self.learning_rate: learn_rate,
                        self.initial_c_state: c_state, self.initial_h_state: h_state})

                # get average loss per window
                training_loss /= len(minibatch[0]) - self.look_back - 1
                print('Training loss: {}'.format(training_loss))
                # increase note limit
                note_limit += note_limit_increase

                if iteration * num_saves % num_iterations == 0:
                    save_path = saver.save(sess, './checkpoints/my-model-{}.ckpt'.format(iteration))
                    print('Saved to {}'.format(save_path))

    def generate(self, checkpoint_file, seed, num_notes):
        generated = [seed]
        saver = tf.train.Saver()

        # pad input so it is accepted even though we only care about the first item
        input_padding = [self.pad_note] * (self.look_back - 1)

        with tf.Session() as sess:
            saver.restore(sess, checkpoint_file)
            c_state = np.zeros([1, self.num_hidden_units]) # batch size is 1
            h_state = np.zeros([1, self.num_hidden_units])

            for i in range(num_notes - 1):
                data_in = [[generated[-1]] + input_padding] # num_steps is 1, batch_size is 1
                pred, c_state, h_state = sess.run([self.next_prediction, self.next_c_state, self.next_h_state],
                    feed_dict={self.x: data_in, self.initial_c_state: c_state,
                    self.initial_h_state: h_state})
                generated.append(pred.tolist()[0])

        return generated

    def make_randomized_padded_minibatch(self, dataset, minibatch_size):
        indices = random.sample(range(len(dataset)), minibatch_size)
        max_length = max(len(dataset[i]) for i in indices)
        minibatch = []
        for i in indices:
            minibatch.append(dataset[i] + [self.pad_note] * (max_length - len(dataset[i])))
        return np.array(minibatch)


# shape of both: [batch_size, features]
def masked_mean_squared_error(y_true, y_pred):
    mask = tf.sign(2.0 - y_true)
    squared_errors = tf.square(y_pred - y_true) * mask
    sum_errors = tf.reduce_sum(squared_errors)
    denom = tf.reduce_sum(mask)
    mse = sum_errors / denom
    return mse

def masked_crossentropy(y_true, y_pred):
    cross_entropy = y_true * tf.log(y_pred)
    cross_entropy = -tf.reduce_sum(cross_entropy, axis=1)
    mask = tf.reduce_max(tf.sign(2.0 - y_true), axis=1)
    cross_entropy *= mask
    cross_entropy = tf.reduce_sum(cross_entropy)
    cross_entropy /= tf.reduce_sum(mask)
    return cross_entropy

# def compound_loss(y_true, y_pred):
#     regression_part_true = tf.concat([y_true[:, 0:3], y_true[:, 15:26]], 1)
#     regression_part_pred = tf.concat([y_pred[:, 0:3], y_pred[:, 15:26]], 1)
#     one_hot_part_true = y_true[:, 3:15]
#     one_hot_part_pred = y_pred[:, 3:15]
#     mse = masked_mean_squared_error(regression_part_true, regression_part_pred)
#     cce = masked_crossentropy(one_hot_part_true, one_hot_part_pred)
#     return mse + cce

def compound_loss(y_true, y_pred):
    one_hot_parts_true = [y_true[:, 3:15], y_true[:, 15:26], y_true[:, 31:37], y_true[:, 38:44], y_true[:, 45:53]]
    one_hot_parts_pred = [y_pred[:, 3:15], y_pred[:, 15:26], y_pred[:, 31:37], y_pred[:, 38:44], y_pred[:, 45:53]]
    cross_entropies = [None] * len(one_hot_parts_true)
    for i in range(len(one_hot_parts_true)):
        cross_entropies[i] = masked_crossentropy(one_hot_parts_true[i], one_hot_parts_pred[i])
    avg_cross_entropy = sum(cross_entropies) / float(len(cross_entropies))

    regressive_part_true = tf.concat([y_true[:, 0:3], y_true[:, 26:31], y_true[:, 37:38], y_true[:, 44:45]], 1)
    regressive_part_pred = tf.concat([y_pred[:, 0:3], y_pred[:, 26:31], y_pred[:, 37:38], y_pred[:, 44:45]], 1)
    mse = masked_mean_squared_error(regressive_part_true, regressive_part_pred)

    return avg_cross_entropy + mse

# # shape of x: [?, features]
# def compound_activation(x):
#     # do softmax on the middle 12, sigmoid on the rest
#     sigmoid_result_1 = tf.nn.sigmoid(x[:, 0:3])
#     softmax_result = tf.nn.softmax(x[:, 3:15])
#     sigmoid_result_2 = tf.nn.sigmoid(x[:, 15:26])
#     # merge the three together
#     return tf.concat([sigmoid_result_1, softmax_result, sigmoid_result_2], 1)

def compound_activation(x):
    sections = []
    # key and mode
    sections.append(tf.nn.sigmoid(x[:, 0:3]))
    # relative pitch
    sections.append(tf.nn.softmax(x[:, 3:15]))
    # octave
    sections.append(tf.nn.softmax(x[:, 15:26]))
    # basis
    sections.append(tf.nn.sigmoid(x[:, 26:31]))
    # onset depth
    sections.append(tf.nn.softmax(x[:, 31:37]))
    # onset increment
    sections.append(tf.nn.sigmoid(x[:, 37:38]))
    # duration depth
    sections.append(tf.nn.softmax(x[:, 38:44]))
    # duration increment
    sections.append(tf.nn.sigmoid(x[:, 44:45]))
    # voice
    sections.append(tf.nn.softmax(x[:, 45:53]))
    return tf.concat(sections, 1)

def make_dataset(filenames):
    data = []
    for filename in filenames:
        try:
            with open(filename) as f:
                piece = []
                for line in f:
                    if (line != ''):
                        piece.append(normalize_line(line))
                data.append(piece)
        except IOError as e:
            print('Data file not found')
            return
    return data


#update: one-hot encode voice, octave and both depths
def normalize_line(line):
    vector = [int(x) for x in line.strip().split('\t')]
    normalized_vector = [0.0] * 53
    # key (map to circle of radius 0.5 centered at (0.5, 0.5))
    key = float(vector[0])/12.0
    normalized_vector[0] = (math.cos(key * 2.0 * math.pi) + 1.0) / 2.0
    normalized_vector[1] = (math.sin(key * 2.0 * math.pi) + 1.0) / 2.0
    # key mode
    normalized_vector[2] = float(vector[1])
    # relative pitch (one-hot)
    normalized_vector[3 + vector[2]] = 1.0
    # octave (one-hot) (add one to index since first octave is -1)
    normalized_vector[15 + vector[3] + 1] = 1.0
    # basis (use homeomorphism from [0, \infty) to [0, 1), and subtract 2 since lowest is 2)
    for i in range(26, 31):
        normalized_vector[i] = math.tanh((float(vector[i-22]) - 2.0 + 0.5) / 2.0)
    # onset depth (one-hot)
    normalized_vector[31 + vector[9]] = 1.0
    # onset increment
    normalized_vector[37] = math.atan((float(vector[10]) + 0.5) / 2.0) / (math.pi/2.0)
    # duration depth (one-hot)
    normalized_vector[38 + vector[11]] = 1.0
    # duration increment
    normalized_vector[44] = math.atan((float(vector[12]) + 0.5) / 2.0) / (math.pi/2.0)
    # voice
    normalized_vector[45 + vector[13]] = 1.0
    return normalized_vector

def denormalize_line(vector):
    denormalized_vector = [0.0] * 14
    # key
    key_x = vector[0] * 2.0 - 1.0
    key_y = vector[1] * 2.0 - 1.0
    pre_key = cmath.polar(complex(key_x, key_y))[1] / (2 * math.pi)
    key = pre_key + 1.0 - int(pre_key + 1.0)
    denormalized_vector[0] = int(round(key * 12.0)) % 12
    # key mode
    denormalized_vector[1] = int(round(vector[2]))
    # relative pitch (scale to sum to 1 in case of softmax roundoff error)
    one_hot_pitch = vector[3:15]
    distribution = list(x/sum(one_hot_pitch) for x in one_hot_pitch)
    pitch = np.random.choice(range(12), p=distribution)
    denormalized_vector[2] = pitch
    # octave
    one_hot_octave = vector[15:26]
    distribution = list(x/sum(one_hot_octave) for x in one_hot_octave)
    octave = np.random.choice(range(11), p=distribution)
    denormalized_vector[3] = octave - 1 # remember octaves start at -1
    # basis
    for i in range(4, 9):
        denormalized_vector[i] = int(round(math.atanh(vector[i+22]) * 2.0 - 0.5 + 2.0))
    # onset depth
    one_hot_onset_depth = vector[31:37]
    distribution = list(x/sum(one_hot_onset_depth) for x in one_hot_onset_depth)
    onset_depth = np.random.choice(range(6), p=distribution)
    denormalized_vector[9] = onset_depth
    # onset increment
    denormalized_vector[10] = int(round(math.tan(vector[37] * math.pi / 2.0) * 2.0 - 0.5))
    # duration depth
    one_hot_duration_depth = vector[38:44]
    distribution = list(x/sum(one_hot_duration_depth) for x in one_hot_duration_depth)
    duration_depth = np.random.choice(range(6), p=distribution)
    denormalized_vector[11] = duration_depth
    # duration increment
    denormalized_vector[12] = int(round(math.tan(vector[44] * math.pi / 2.0) * 2.0 - 0.5))
    # voice
    one_hot_voice = vector[45:53]
    distribution = list(x/sum(one_hot_voice) for x in one_hot_voice)
    voice = np.random.choice(range(8), p=distribution)
    denormalized_vector[13] = voice

    return reduce(lambda x, y : str(x) + '\t' + str(y), denormalized_vector)

def denormalize_line_deterministic(vector):
    denormalized_vector = [0.0] * 14
    # key
    key_x = vector[0] * 2.0 - 1.0
    key_y = vector[1] * 2.0 - 1.0
    pre_key = cmath.polar(complex(key_x, key_y))[1] / (2 * math.pi)
    key = pre_key + 1.0 - int(pre_key + 1.0)
    denormalized_vector[0] = int(round(key * 12.0)) % 12
    # key mode
    denormalized_vector[1] = int(round(vector[2]))
    # relative pitch (scale to sum to 1 in case of softmax roundoff error)
    one_hot_pitch = vector[3:15]
    denormalized_vector[2] = argmax(one_hot_pitch)
    # octave
    one_hot_octave = vector[15:26]
    denormalized_vector[3] = argmax(one_hot_octave) - 1 # remember octaves start at -1
    # basis
    for i in range(4, 9):
        denormalized_vector[i] = int(round(math.atanh(vector[i+22]) * 2.0 - 0.5 + 2.0))
    # onset depth
    one_hot_onset_depth = vector[31:37]
    denormalized_vector[9] = argmax(one_hot_onset_depth)
    # onset increment
    denormalized_vector[10] = int(round(math.tan(vector[37] * math.pi / 2.0) * 2.0 - 0.5))
    # duration depth
    one_hot_duration_depth = vector[38:44]
    denormalized_vector[11] = argmax(one_hot_duration_depth)
    # duration increment
    denormalized_vector[12] = int(round(math.tan(vector[44] * math.pi / 2.0) * 2.0 - 0.5))
    # voice
    one_hot_voice = vector[45:53]
    denormalized_vector[13] = argmax(one_hot_voice)

    return reduce(lambda x, y : str(x) + '\t' + str(y), denormalized_vector)

def check_normalization(filename):
    with open(filename) as f:
        for line in f:
            norm = normalize_line(line)
            denorm = denormalize_line(norm)
            assert denorm.strip() == line.strip()

def argmax(l):
    return max(range(len(l)), key=lambda i : l[i])

if __name__ == '__main__':
    main()
