#!/usr/bin/python3

import sys

def kernel_one(a, b, k):
  ans = 0
  for i in range(0, len(a) - k + 1):
    for j in range(0, len(b) - k + 1):
      if a[i:i+k] == b[j:j+k]:
        ans += 1
  return ans

# Direct implementation of KMP. Runtime: O(|text| + |pattern|).
def kmp_occurences(text, pattern):
  pi = [-1, 0]
  for i in range(1, len(pattern)):
    pos = pi[-1]
    while pos >= 0 and pattern[i] != pattern[pos + 1]:
      pos = pi[pos]
    pi.append(pos + 1)
  ans = 0; pos = 0
  for c in text:
    while pos >= 0 and c != pattern[pos]:
      pos = pi[pos]
    pos += 1
    if pos == len(pattern):
      ans += 1
      pos = pi[pos]
  return ans

def kernel_two(a, b, k):
  ans = 0
  for i in range(0, len(a) - k + 1):
    ans += kmp_occurences(b, a[i:i+k])
  return ans

def char_to_int(c):
  return ord(c) - ord('a') + 1

prime = 253210004963
print(prime)
def get_rolling_hashes(s, k):
  coef = (26 ** k) % prime
  hash = 0
  for i in range(0, k):
    hash = (hash * 26 + char_to_int(s[i])) % prime
  yield hash
  for i in range(k, len(s)):
    delta = char_to_int(s[i]) - coef * char_to_int(s[i-k])
    hash = (hash * 26 + delta) % prime
    yield hash

def kernel_three(a, b, k):
  hashes = {}
  for hash in get_rolling_hashes(a, k):
    hashes[hash] = hashes.get(hash, 0) + 1
  ans = 0;
  for hash in get_rolling_hashes(b, k):
    ans += hashes.get(hash, 0)
  return ans

def kernel_1(a, b):
  freq = {}
  for c in a:
    freq[c] = freq.get(c, 0) + 1
  ans = 0
  for c in b:
    ans += freq.get(c, 0)
  return ans

def kernel(a, b, K):
  d = [[[0 for _ in range(len(b) + 1)] for _ in range(len(a) + 1)] for _ in range(K + 1)]
  for i in range(1, len(a) + 1):
    for j in range(1, len(b) + 1):
      d[1][i][j] = kernel_1(a[0:i], b[0:j])
      print("d[1][{}][{}] = {}".format(i, j, d[1][i][j]))
  print(d)
  for k in range(2, K + 1):
    for i in range(k, len(a) + 1):
      for j in range(k, len(b) + 1):
        print("d[{}][{}][{}] = {}".format(k-1, i-1, j-1, d[k-1][i-1][j-1]))
        d[k][i][j] = d[k-1][i-1][j-1] * (a[i-1] == b[j-1]) + d[k][i - 1][j] + d[k][i][j-1] - d[k][i-1][j-1]
        print("d[{}][{}][{}] = {}".format(k, i, j, d[k][i][j]))
  return d[k][len(a)][len(b)]

_,a,b,k_str=sys.argv
k = int(k_str)

print("a = {}; b = {}; k = {}".format(a, b, k))
print("Approach: {}".format(kernel(a, b, k)))
#print("Approach 2: {}".format(kernel_two(a, b, k)))
#print("Approach 3: {}".format(kernel_three(a, b, k)))
