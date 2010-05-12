import string

class enum(object):
    __init = 1
    def __init__(self, *args, **kw):
        value = 0
        self.__names = args
        self.__dict = {}
        for name in args:
            if kw.has_key(name):
                value = kw[name]
            self.__dict[name] = value
            value = value + 1
        self.__init = 0

    def __getitem__(self, name):
        return getattr(self, name)

    def __getattr__(self, name):
        return self.__dict[name]

    def __setattr__(self, name, value):
        if self.__init:
            self.__dict__[name] = value
        else:
            raise AttributeError, "enum is ReadOnly"

    def __call__(self, name_or_value):
        if type(name_or_value) == type(0):
            for name in self.__names:
                if getattr(self, name) == name_or_value:
                    return name
            else:
                raise TypeError, "no enum for %d" % name_or_value
        else:
            return getattr(self, name_or_value)

    def __repr__(self):
        result = ['<enum']
        for name in self.__names:
            result.append("%s=%d" % (name, getattr(self, name)))
        return string.join(result)+'>'

    def __len__(self):
        return len(self.__dict)

    def __iter__(self):
        print 'iter'
        return self.__dict.__iter__()

def test():
    color = enum('red', 'yellow', 'green', 'blue', green=20)
    print color
    print color.yellow
    print color['yellow']
    print color(21)
    for col in color:
        print color(col), '=', col

    print "Len: %d" % len(color)

    for k in color:
        print k,color[k]

if __name__ == '__main__':
    test()
