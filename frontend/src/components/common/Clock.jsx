import React, { useEffect, useState } from 'react'

const styles = {
  wrapper: {
    margin: 8,
    padding: 8,
    display: "flex",
    flexDirection: "row",
    border: "1px solid black",
    alignItems: "center",
    borderRadius: 16,
  },
  messageText: {
    color: "black",
    fontSize: 16,
    fontWeight: "bold",
  }
};

function Clock(props) {
    const [alarm, setAlarm] = useState(false);
    const [start, setStart] = useState(false);
    const [time, setTime, increaseCount, decreaseCount] = useState(3600);

    function handleStart() {
        setStart((start) => !start);
    }

    useEffect(() => {
        const timer = setInterval(() => {
            if(start){
                setTime(time - 1);
            };
        }, 1000);

        if (time === 0 && start) {
            setAlarm(true);
            setStart(false);
            clearInterval(timer);
        };
        return () => clearInterval(timer);
    }, [time, start]);

    return (
        <div>
            <h3>{`${Math.floor((time / (3600)) % 24)}시 ${Math.floor((time / 60) % 60)}분 ${Math.floor(time % 60)}초 남음`}</h3>
            <button onClick={handleStart} disabled={alarm} >{start ? "정지" : "시작"}</button>
            <button onClick={() => [setTime(3600), setAlarm(false), setStart(false)]}>초기화</button>
            <button onClick={() => setTime(time + 1)} disabled={start || alarm}>+1</button>
            <button onClick={() => setTime(time - 1)} disabled={start || alarm}>-1</button>
            {alarm && <p style={{color: "red"}}>알람 끗!</p>}
        </div>);
}

// class Clock extends React.Component {
//   constructor(props) {
//     super(props);
//     this.state = {
//         target: new Date("9/16/2025 00:00:00").getTime(),
//     };
//   }

//   componentDidMount() {
//     const { target } = this.state;
    
//     timer = setInterval(() => {
//         const remine = target - new Date().getTime();

//         const sec = Math.floor((remine / 1000) % 60); // 초
//         const min = Math.floor((remine / (1000 * 60)) % 60); // 분
//         const hours = Math.floor((remine / (1000 * 60 * 60)) % 24); // 시간
//         const days = Math.floor((remine / (1000 * 60 * 60 * 24))); // 일

//         this.setState({
//             remine : `${days}일 ${hours}시간 ${min}분 ${sec}초 남았습니다.`
//         });
//     }, 1000)
//   }

//   render() {
//     return (
//         <div>
//             <span style={styles.messageText}>{this.state.remine}</span>
//         </div>
//     );
//   }
// }

export default Clock;