union () {
  translate ([0, 30, 0]) {
    union () {
      translate ([0, 0, 3.06]) {
        translate ([41/2, 0, 0]) {
          difference () {
            union () {
              cylinder (h=6.12, r=8, center=true);
            }
            translate ([3, 0, 0]) {
              cylinder (h=100, r=1, center=true);
            }
            translate ([-50, 0, 0]) {
              cube ([100, 100, 100], center=true);
            }
          }
        }
      }
      difference () {
        translate ([0, 0, 2]) {
          difference () {
            cube ([41, 25.8, 4], center=true);
            translate ([0, 0, 1.2]) {
              union () {
                translate ([0, 10.9, 0.4]) {
                  cube ([34, 0.8500000000000001, 0.8500000000000001], center=true);
                }
                translate ([0, -10.9, 0.4]) {
                  cube ([34, 0.8500000000000001, 0.8500000000000001], center=true);
                }
              }
            }
            translate ([-11.5, 7.62, 0]) {
              cube ([6, 6, 10], center=true);
            }
          }
        }
        translate ([0, 0, 2.2025]) {
          union () {
            translate ([0, 7.62, 0]) {
              union () {
                translate ([-17.78, 0, 0]) {
                  cylinder (h=3.6049999999999995, r=0.5, center=true);
                }
                translate ([-15.24, 0, 0]) {
                  cylinder (h=3.6049999999999995, r=0.5, center=true);
                }
                translate ([-12.7, 0, 0]) {
                  cylinder (h=3.6049999999999995, r=0.5, center=true);
                }
                translate ([-10.16, 0, 0]) {
                  cylinder (h=3.6049999999999995, r=0.5, center=true);
                }
                translate ([-7.62, 0, 0]) {
                  cylinder (h=3.6049999999999995, r=0.5, center=true);
                }
                translate ([-5.08, 0, 0]) {
                  cylinder (h=3.6049999999999995, r=0.5, center=true);
                }
                translate ([-2.54, 0, 0]) {
                  cylinder (h=3.6049999999999995, r=0.5, center=true);
                }
                translate ([0.0, 0, 0]) {
                  cylinder (h=3.6049999999999995, r=0.5, center=true);
                }
                translate ([2.54, 0, 0]) {
                  cylinder (h=3.6049999999999995, r=0.5, center=true);
                }
                translate ([5.08, 0, 0]) {
                  cylinder (h=3.6049999999999995, r=0.5, center=true);
                }
                translate ([7.62, 0, 0]) {
                  cylinder (h=3.6049999999999995, r=0.5, center=true);
                }
                translate ([10.16, 0, 0]) {
                  cylinder (h=3.6049999999999995, r=0.5, center=true);
                }
                translate ([12.7, 0, 0]) {
                  cylinder (h=3.6049999999999995, r=0.5, center=true);
                }
                translate ([15.24, 0, 0]) {
                  cylinder (h=3.6049999999999995, r=0.5, center=true);
                }
              }
            }
            translate ([0, -7.62, 0]) {
              union () {
                translate ([-17.78, 0, 0]) {
                  cylinder (h=3.6049999999999995, r=0.5, center=true);
                }
                translate ([-15.24, 0, 0]) {
                  cylinder (h=3.6049999999999995, r=0.5, center=true);
                }
                translate ([-12.7, 0, 0]) {
                  cylinder (h=3.6049999999999995, r=0.5, center=true);
                }
                translate ([-10.16, 0, 0]) {
                  cylinder (h=3.6049999999999995, r=0.5, center=true);
                }
                translate ([-7.62, 0, 0]) {
                  cylinder (h=3.6049999999999995, r=0.5, center=true);
                }
                translate ([-5.08, 0, 0]) {
                  cylinder (h=3.6049999999999995, r=0.5, center=true);
                }
                translate ([-2.54, 0, 0]) {
                  cylinder (h=3.6049999999999995, r=0.5, center=true);
                }
                translate ([0.0, 0, 0]) {
                  cylinder (h=3.6049999999999995, r=0.5, center=true);
                }
                translate ([2.54, 0, 0]) {
                  cylinder (h=3.6049999999999995, r=0.5, center=true);
                }
                translate ([5.08, 0, 0]) {
                  cylinder (h=3.6049999999999995, r=0.5, center=true);
                }
                translate ([7.62, 0, 0]) {
                  cylinder (h=3.6049999999999995, r=0.5, center=true);
                }
                translate ([10.16, 0, 0]) {
                  cylinder (h=3.6049999999999995, r=0.5, center=true);
                }
                translate ([12.7, 0, 0]) {
                  cylinder (h=3.6049999999999995, r=0.5, center=true);
                }
                translate ([15.24, 0, 0]) {
                  cylinder (h=3.6049999999999995, r=0.5, center=true);
                }
              }
            }
          }
        }
      }
    }
  }
  union () {
    translate ([0, 0, 3.06]) {
      translate ([41/2, 0, 0]) {
        difference () {
          union () {
            cylinder (h=6.12, r=8, center=true);
          }
          translate ([3, 0, 1]) {
            cylinder (h=6.12, r=2.655, center=true);
          }
          translate ([-50, 0, 0]) {
            cube ([100, 100, 100], center=true);
          }
        }
      }
    }
    translate ([0, 0, 6.12]) {
      union () {
        translate ([0, 10.9, 0]) {
          cube ([41, 4, 4.24], center=true);
        }
        translate ([0, 10.9, 2.52]) {
          cube ([33, 0.8, 0.8], center=true);
        }
        translate ([0, -10.9, 0]) {
          cube ([41, 4, 4.24], center=true);
        }
        translate ([0, -10.9, 2.52]) {
          cube ([33, 0.8, 0.8], center=true);
        }
        translate ([37/2, 0, 0]) {
          cube ([4, 25.8, 4.24], center=true);
        }
      }
    }
    difference () {
      translate ([0, 0, 2]) {
        cube ([41, 25.8, 4], center=true);
      }
      translate ([0, 0, 2.2025]) {
        union () {
          translate ([0, 7.62, 0]) {
            union () {
              translate ([-17.78, 0, 0]) {
                cylinder (h=3.6049999999999995, r=0.5, center=true);
              }
              translate ([-15.24, 0, 0]) {
                cylinder (h=3.6049999999999995, r=0.5, center=true);
              }
              translate ([-12.7, 0, 0]) {
                cylinder (h=3.6049999999999995, r=0.5, center=true);
              }
              translate ([-10.16, 0, 0]) {
                cylinder (h=3.6049999999999995, r=0.5, center=true);
              }
              translate ([-7.62, 0, 0]) {
                cylinder (h=3.6049999999999995, r=0.5, center=true);
              }
              translate ([-5.08, 0, 0]) {
                cylinder (h=3.6049999999999995, r=0.5, center=true);
              }
              translate ([-2.54, 0, 0]) {
                cylinder (h=3.6049999999999995, r=0.5, center=true);
              }
              translate ([0.0, 0, 0]) {
                cylinder (h=3.6049999999999995, r=0.5, center=true);
              }
              translate ([2.54, 0, 0]) {
                cylinder (h=3.6049999999999995, r=0.5, center=true);
              }
              translate ([5.08, 0, 0]) {
                cylinder (h=3.6049999999999995, r=0.5, center=true);
              }
              translate ([7.62, 0, 0]) {
                cylinder (h=3.6049999999999995, r=0.5, center=true);
              }
              translate ([10.16, 0, 0]) {
                cylinder (h=3.6049999999999995, r=0.5, center=true);
              }
              translate ([12.7, 0, 0]) {
                cylinder (h=3.6049999999999995, r=0.5, center=true);
              }
              translate ([15.24, 0, 0]) {
                cylinder (h=3.6049999999999995, r=0.5, center=true);
              }
            }
          }
          translate ([0, -7.62, 0]) {
            union () {
              translate ([-17.78, 0, 0]) {
                cylinder (h=3.6049999999999995, r=0.5, center=true);
              }
              translate ([-15.24, 0, 0]) {
                cylinder (h=3.6049999999999995, r=0.5, center=true);
              }
              translate ([-12.7, 0, 0]) {
                cylinder (h=3.6049999999999995, r=0.5, center=true);
              }
              translate ([-10.16, 0, 0]) {
                cylinder (h=3.6049999999999995, r=0.5, center=true);
              }
              translate ([-7.62, 0, 0]) {
                cylinder (h=3.6049999999999995, r=0.5, center=true);
              }
              translate ([-5.08, 0, 0]) {
                cylinder (h=3.6049999999999995, r=0.5, center=true);
              }
              translate ([-2.54, 0, 0]) {
                cylinder (h=3.6049999999999995, r=0.5, center=true);
              }
              translate ([0.0, 0, 0]) {
                cylinder (h=3.6049999999999995, r=0.5, center=true);
              }
              translate ([2.54, 0, 0]) {
                cylinder (h=3.6049999999999995, r=0.5, center=true);
              }
              translate ([5.08, 0, 0]) {
                cylinder (h=3.6049999999999995, r=0.5, center=true);
              }
              translate ([7.62, 0, 0]) {
                cylinder (h=3.6049999999999995, r=0.5, center=true);
              }
              translate ([10.16, 0, 0]) {
                cylinder (h=3.6049999999999995, r=0.5, center=true);
              }
              translate ([12.7, 0, 0]) {
                cylinder (h=3.6049999999999995, r=0.5, center=true);
              }
              translate ([15.24, 0, 0]) {
                cylinder (h=3.6049999999999995, r=0.5, center=true);
              }
            }
          }
        }
      }
    }
  }
}
